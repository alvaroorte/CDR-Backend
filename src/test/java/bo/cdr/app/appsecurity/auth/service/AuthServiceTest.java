package bo.cdr.app.appsecurity.auth.service;

import bo.cdr.app.appsecurity.auth.jwt.JwtService;
import bo.cdr.app.appsecurity.domain.entity.RefreshToken;
import bo.cdr.app.appsecurity.domain.entity.Role;
import bo.cdr.app.appsecurity.domain.entity.UserApp;
import bo.cdr.app.appsecurity.domain.entity.UserRole;
import bo.cdr.app.appsecurity.domain.service.interfaces.RefreshTokenService;
import bo.cdr.app.appsecurity.domain.service.interfaces.UserRoleService;
import bo.cdr.app.appsecurity.domain.service.interfaces.UserService;
import bo.cdr.app.appsecurity.presentation.request.dto.UserCredentialsDto;
import bo.cdr.app.appsecurity.presentation.response.pojo.AuthPojo;
import bo.cdr.app.exception.TooManyLoginAttemptsException;
import bo.cdr.app.exception.UnauthorizedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock AuthenticationManager authenticationManager;
    @Mock UserService userService;
    @Mock UserRoleService userRoleService;
    @Mock JwtService jwtService;
    @Mock RefreshTokenService refreshTokenService;
    @Mock LoginRateLimiterService loginRateLimiterService;

    private static final String IP = "1.2.3.4";

    AuthService authService() {
        return new AuthService(authenticationManager, userService, userRoleService,
                jwtService, refreshTokenService, loginRateLimiterService);
    }

    private UserCredentialsDto request(String rawPassword) {
        UserCredentialsDto dto = new UserCredentialsDto();
        dto.setUsername(" maria.caja ");
        dto.setPassword(Base64.getEncoder().encodeToString(rawPassword.getBytes(StandardCharsets.UTF_8)));
        return dto;
    }

    private UserApp user() {
        UserApp u = new UserApp();
        u.setId(7);
        u.setUsername("maria.caja");
        u.setFullName("María Caja");
        return u;
    }

    private UserRole userRole(String code) {
        Role role = new Role();
        role.setCode(code);
        UserRole ur = new UserRole();
        ur.setRole(role);
        return ur;
    }

    @Test
    void login_returnsTokens_andDecodesBase64Password_whenCredentialsValid() {
        UserApp user = user();
        when(userService.findActiveByUsername("maria.caja")).thenReturn(Optional.of(user));
        when(userRoleService.getRolesByUser(user)).thenReturn(List.of(userRole("CAJERO")));
        when(jwtService.generateToken(eq(user), any())).thenReturn("jwt-token");
        when(refreshTokenService.issue(user)).thenReturn("refresh-token");

        AuthPojo result = authService().login(request("Clave#123"), IP);

        assertThat(result.getToken()).isEqualTo("jwt-token");
        assertThat(result.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(result.getUsername()).isEqualTo("maria.caja");
        // username recortado y password decodificado antes de autenticar
        verify(authenticationManager).authenticate(new UsernamePasswordAuthenticationToken("maria.caja", "Clave#123"));
        verify(loginRateLimiterService).reset("maria.caja", IP);
    }

    @Test
    void login_recordsFailure_andRethrows_whenBadCredentials() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService().login(request("mala"), IP))
                .isInstanceOf(BadCredentialsException.class);

        verify(loginRateLimiterService).recordFailure("maria.caja", IP);
        verifyNoInteractions(jwtService, refreshTokenService);
    }

    @Test
    void login_recordsFailure_whenUserDisabled() {
        when(authenticationManager.authenticate(any())).thenThrow(new DisabledException("disabled"));

        assertThatThrownBy(() -> authService().login(request("Clave#123"), IP))
                .isInstanceOf(DisabledException.class);

        verify(loginRateLimiterService).recordFailure("maria.caja", IP);
    }

    @Test
    void login_neverAuthenticates_whenRateLimited() {
        doThrow(new TooManyLoginAttemptsException("Demasiados intentos"))
                .when(loginRateLimiterService).checkNotBlocked("maria.caja", IP);

        assertThatThrownBy(() -> authService().login(request("Clave#123"), IP))
                .isInstanceOf(TooManyLoginAttemptsException.class);

        verifyNoInteractions(authenticationManager);
    }

    @Test
    void decodeBase64Password_throwsBadCredentials_whenNotBase64() {
        assertThatThrownBy(() -> AuthService.decodeBase64Password("%%%no-base64%%%"))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void refresh_rotatesToken_whenUserActive() {
        UserApp user = user();
        RefreshToken stored = RefreshToken.builder().username("maria.caja").build();
        when(refreshTokenService.validateUsable("old")).thenReturn(stored);
        when(userService.findActiveByUsername("maria.caja")).thenReturn(Optional.of(user));
        when(userRoleService.getRolesByUser(user)).thenReturn(List.of(userRole("CAJERO")));
        when(jwtService.generateToken(eq(user), any())).thenReturn("new-jwt");
        when(refreshTokenService.issue(user)).thenReturn("new-refresh");

        AuthPojo result = authService().refresh("old");

        assertThat(result.getToken()).isEqualTo("new-jwt");
        assertThat(result.getRefreshToken()).isEqualTo("new-refresh");
        verify(refreshTokenService).revoke(stored);
    }

    @Test
    void refresh_revokesAndThrows_whenUserInactive() {
        RefreshToken stored = RefreshToken.builder().username("maria.caja").build();
        when(refreshTokenService.validateUsable("old")).thenReturn(stored);
        when(userService.findActiveByUsername("maria.caja")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService().refresh("old"))
                .isInstanceOf(UnauthorizedException.class);

        verify(refreshTokenService).revoke(stored);
        verify(refreshTokenService, never()).issue(any());
    }
}
