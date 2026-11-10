package bo.cdr.app.appsecurity.auth.jwt;

import bo.cdr.app.appsecurity.auth.service.CustomUserDetails;
import bo.cdr.app.appsecurity.domain.service.UserService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthorizationFilterTest {

    @Mock UserService userService;
    @Mock FilterChain chain;

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(String role) {
        CustomUserDetails user = CustomUserDetails.builder()
                .username("u").userId(5).password("")
                .authorities(List.of(new SimpleGrantedAuthority(role)))
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    private MockHttpServletRequest request(String method, String uri) {
        MockHttpServletRequest req = new MockHttpServletRequest(method, "/cdr" + uri);
        req.setContextPath("/cdr");
        req.setServletPath(uri);
        return req;
    }

    @Test
    void denies_whenUserHasNoPermission_andIsNotAdmin() {
        authenticateAs("CAJERO");
        when(userService.hasPermission(5, "DELETE", "/product")).thenReturn(false);

        assertThatThrownBy(() -> new AuthorizationFilter(userService)
                .doFilter(request("DELETE", "/product/12"), new MockHttpServletResponse(), chain))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void allows_andNormalizesNumericIds_whenUserHasPermission() throws Exception {
        authenticateAs("COCINA");
        when(userService.hasPermission(5, "PATCH", "/order/status")).thenReturn(true);

        new AuthorizationFilter(userService)
                .doFilter(request("PATCH", "/order/33/status"), new MockHttpServletResponse(), chain);

        verify(chain).doFilter(any(), any());
    }

    @Test
    void allows_whenAdmin_evenWithoutExplicitPermission() throws Exception {
        authenticateAs("ADMIN");
        when(userService.hasPermission(any(), any(), any())).thenReturn(false);

        new AuthorizationFilter(userService)
                .doFilter(request("DELETE", "/product/12"), new MockHttpServletResponse(), chain);

        verify(chain).doFilter(any(), any());
    }

    @Test
    void skipsPermissionCheck_forAuthenticatedOnlyRoutes() throws Exception {
        authenticateAs("COCINA");

        new AuthorizationFilter(userService)
                .doFilter(request("GET", "/users/me"), new MockHttpServletResponse(), chain);

        verify(chain).doFilter(any(), any());
        verifyNoInteractions(userService);
    }
}
