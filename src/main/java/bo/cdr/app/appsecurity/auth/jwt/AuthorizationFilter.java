package bo.cdr.app.appsecurity.auth.jwt;

import bo.cdr.app.appsecurity.auth.service.CustomUserDetails;
import bo.cdr.app.appsecurity.domain.service.interfaces.UserService;
import bo.cdr.app.util.RequestMatcherUtil;
import bo.cdr.app.util.constants.RoleCodes;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthorizationFilter extends OncePerRequestFilter {

    private final UserService userService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // 1. Endpoints públicos (sin token)
        if (RequestMatcherUtil.isTokenNotRequired(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Endpoints sin permisos (solo token)
        if (RequestMatcherUtil.isPermissionNotRequired(request)) {
            filterChain.doFilter(request, response);
            log.debug("Permission-exempt endpoint: {}", request.getRequestURI());
            return;
        }

        // 3. Obtener autenticación
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }

        // 4. Obtener usuario
        Object principal = authentication.getPrincipal();

        if (!(principal instanceof CustomUserDetails userDetails)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String url = normalizeUrl(request);
            String method = request.getMethod();

            Integer userId = userDetails.getUserId();

            boolean hasPermission = userService.hasPermission(userId, method, url);

            boolean isAdmin = userDetails.getAuthorities()
                    .stream()
                    .anyMatch(auth -> auth.getAuthority().equals(RoleCodes.ADMIN));

            if (!hasPermission && !isAdmin) {
                throw new AccessDeniedException("No tienes permisos para acceder");
            }

            filterChain.doFilter(request, response);

        } catch (AccessDeniedException ex) {
            throw ex; // lo maneja Spring (403)
        }
    }

    private String normalizeUrl(HttpServletRequest request) {

        String url = request.getRequestURI();
        String context = request.getContextPath();

        // 1. quitar context path
        url = url.substring(context.length());

        // 2. quitar segmentos SOLO numéricos
        url = url.replaceAll("/\\d+(?=/|$)", "");

        // 3. quitar slash final
        if (url.length() > 1 && url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }

        return url;
    }

}