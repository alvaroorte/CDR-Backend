package bo.cdr.app.appsecurity.auth.jwt;

import bo.cdr.app.appsecurity.auth.service.CustomUserDetails;
import bo.cdr.app.appsecurity.data.UserRoleRepository;
import bo.cdr.app.util.RequestMatcherUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Autenticación por JWT (referencia). Desviación de la Etapa 7: el token solo identifica al usuario;
 * <b>si está activo y qué roles tiene se lee de la BD en cada petición</b>. Así, desactivar a un
 * usuario o quitarle el rol ADMIN surte efecto inmediato y no al caducar el JWT (12 h).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRoleRepository userRoleRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        if (RequestMatcherUtil.isTokenNotRequired(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = jwtService.extractToken(request);

        if (token != null && jwtService.validateToken(token)) {
            Authentication authentication = buildAuthentication(token);
            if (authentication != null) {
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Construye la autenticación a partir de un JWT válido (reutilizado por el canal STOMP).
     *
     * @return null si el usuario ya no está activo o no tiene ningún rol vigente (→ 401)
     */
    public Authentication buildAuthentication(String token) {
        Integer userId = jwtService.extractUserId(token);
        List<SimpleGrantedAuthority> authorities = userRoleRepository.findActiveRoleCodesOfActiveUser(userId).stream()
                .distinct()
                .map(SimpleGrantedAuthority::new)
                .toList();
        if (authorities.isEmpty()) {
            log.warn("JWT de un usuario inactivo o sin roles vigentes rechazado: userId={}", userId);
            return null;
        }

        CustomUserDetails userDetails = CustomUserDetails.builder()
                .username(jwtService.extractUsername(token))
                .fullName(jwtService.extractFullName(token))
                .password("")
                .userId(userId)
                .authorities(authorities)
                .build();

        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }
}
