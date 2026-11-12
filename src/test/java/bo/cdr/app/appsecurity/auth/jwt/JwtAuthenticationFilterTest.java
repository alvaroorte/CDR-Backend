package bo.cdr.app.appsecurity.auth.jwt;

import bo.cdr.app.appsecurity.auth.service.CustomUserDetails;
import bo.cdr.app.appsecurity.data.UserRoleRepository;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Etapa 7: los roles salen de la BD en cada petición, no del JWT. */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock JwtService jwtService;
    @Mock UserRoleRepository userRoleRepository;
    @Mock FilterChain chain;
    JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtService, userRoleRepository);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/cdr/order/board");
        request.setContextPath("/cdr");
        request.setServletPath("/order/board");
        request.addHeader("Authorization", "Bearer tok");
        return request;
    }

    private void validToken() {
        when(jwtService.extractToken(any(jakarta.servlet.http.HttpServletRequest.class))).thenReturn("tok");
        when(jwtService.validateToken("tok")).thenReturn(true);
        when(jwtService.extractUserId("tok")).thenReturn(7);
    }

    @Test
    void usesCurrentRolesFromDatabase_notTheOnesInTheToken() throws Exception {
        validToken();
        when(jwtService.extractUsername("tok")).thenReturn("maria.caja");
        when(userRoleRepository.findActiveRoleCodesOfActiveUser(7)).thenReturn(List.of("CAJERO"));

        filter.doFilter(request(), new MockHttpServletResponse(), chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getAuthorities()).extracting(GrantedAuthority::getAuthority).containsExactly("CAJERO");
        assertThat(((CustomUserDetails) auth.getPrincipal()).getUserId()).isEqualTo(7);
        verify(chain).doFilter(any(), any());
    }

    @Test
    void deactivatedUser_withStillValidToken_isNotAuthenticated() throws Exception {
        validToken();
        when(userRoleRepository.findActiveRoleCodesOfActiveUser(7)).thenReturn(List.of());

        filter.doFilter(request(), new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(any(), any());
    }

    @Test
    void invalidToken_doesNotQueryDatabase() throws Exception {
        when(jwtService.extractToken(any(jakarta.servlet.http.HttpServletRequest.class))).thenReturn("bad");
        when(jwtService.validateToken("bad")).thenReturn(false);

        filter.doFilter(request(), new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(userRoleRepository);
    }
}
