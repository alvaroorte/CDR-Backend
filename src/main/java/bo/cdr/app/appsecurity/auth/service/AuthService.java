package bo.cdr.app.appsecurity.auth.service;

import bo.cdr.app.appsecurity.auth.jwt.JwtService;
import bo.cdr.app.appsecurity.domain.entity.RefreshToken;
import bo.cdr.app.appsecurity.domain.entity.UserApp;
import bo.cdr.app.appsecurity.domain.service.RefreshTokenService;
import bo.cdr.app.appsecurity.domain.service.UserRoleService;
import bo.cdr.app.appsecurity.domain.service.UserService;
import bo.cdr.app.appsecurity.presentation.request.dto.UserCredentialsDto;
import bo.cdr.app.appsecurity.presentation.response.pojo.AuthPojo;
import bo.cdr.app.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collection;

/**
 * Login, refresh y logout. Igual que la referencia sin reCAPTCHA, cliente de confianza ni
 * sincronización LDAP. El límite de intentos se mantiene.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final UserRoleService userRoleService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final LoginRateLimiterService loginRateLimiterService;

    public AuthPojo login(UserCredentialsDto request, String remoteIp) {

        String username = request.getUsername().trim();
        loginRateLimiterService.checkNotBlocked(username, remoteIp);

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, decodeBase64Password(request.getPassword()))
            );
        } catch (AuthenticationException e) {
            loginRateLimiterService.recordFailure(username, remoteIp);
            throw e;
        }

        loginRateLimiterService.reset(username, remoteIp);

        UserApp user = userService.findActiveByUsername(username)
                .orElseThrow(() -> new UnauthorizedException("Usuario inactivo o no encontrado"));

        log.info("Inicio de sesión: {}", user.getUsername());
        return buildAuthPojo(user, refreshTokenService.issue(user),
                "Hola %s, has iniciado sesión con éxito!".formatted(user.getFullName()));
    }

    /**
     * Renueva el JWT a partir de un refresh token válido. Valida en cada refresh que el usuario
     * siga activo; si fue desactivado se rechaza y el token se revoca.
     */
    public AuthPojo refresh(String rawRefreshToken) {

        RefreshToken stored = refreshTokenService.validateUsable(rawRefreshToken);

        UserApp user = userService.findActiveByUsername(stored.getUsername())
                .orElseThrow(() -> {
                    refreshTokenService.revoke(stored);
                    return new UnauthorizedException("Usuario inactivo o no encontrado");
                });

        // Rotación: revocar el token usado y emitir uno nuevo.
        refreshTokenService.revoke(stored);
        return buildAuthPojo(user, refreshTokenService.issue(user), "Token renovado");
    }

    /** Revoca el refresh token (idempotente). */
    public void logout(String rawRefreshToken) {
        refreshTokenService.revokeByRawToken(rawRefreshToken);
    }

    private AuthPojo buildAuthPojo(UserApp user, String refreshToken, String message) {
        AuthPojo response = new AuthPojo();
        response.setToken(jwtService.generateToken(user, buildAuthorities(user)));
        response.setRefreshToken(refreshToken);
        response.setUsername(user.getUsername());
        response.setMessage(message);
        return response;
    }

    private Collection<SimpleGrantedAuthority> buildAuthorities(UserApp user) {
        return userRoleService.getRolesByUser(user).stream()
                .map(userRole -> new SimpleGrantedAuthority(userRole.getRole().getCode()))
                .toList();
    }

    /** El frontend envía la contraseña en base64 (patrón de la referencia). */
    public static String decodeBase64Password(String input) {
        if (input == null || input.isBlank()) {
            throw new BadCredentialsException("Contraseña incorrecta");
        }
        try {
            return new String(Base64.getDecoder().decode(input), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new BadCredentialsException("Contraseña incorrecta");
        }
    }
}
