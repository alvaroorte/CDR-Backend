package bo.cdr.app.appsecurity.auth.jwt;

import bo.cdr.app.appsecurity.auth.service.CustomUserDetails;
import bo.cdr.app.util.RequestMatcherUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

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
            SecurityContextHolder.getContext().setAuthentication(buildAuthentication(token));
        }

        filterChain.doFilter(request, response);
    }

    /** Construye la autenticación a partir de un JWT válido (reutilizado por el canal STOMP). */
    public Authentication buildAuthentication(String token) {
        List<SimpleGrantedAuthority> authorities = jwtService.extractAuthorities(token).stream()
                .map(SimpleGrantedAuthority::new)
                .toList();

        CustomUserDetails userDetails = CustomUserDetails.builder()
                .username(jwtService.extractUsername(token))
                .fullName(jwtService.extractFullName(token))
                .password("")
                .userId(jwtService.extractUserId(token))
                .authorities(authorities)
                .build();

        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }
}
