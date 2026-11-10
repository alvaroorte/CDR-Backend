package bo.cdr.app.appsecurity.presentation.controller;

import bo.cdr.app.appsecurity.auth.service.AuthService;
import bo.cdr.app.appsecurity.presentation.response.pojo.AuthPojo;
import bo.cdr.app.exception.TooManyLoginAttemptsException;
import bo.cdr.app.util.MockMvcTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    AuthService authService;

    MockMvc mockMvc;

    private static final String VALID_JSON = """
            { "username": "maria.caja", "password": "Q2xhdmUjMTIz" }
            """;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcTestSupport.buildMockMvc(new AuthController(authService));
    }

    @Test
    void login_returns200_whenCredentialsValid() throws Exception {
        when(authService.login(any(), any()))
                .thenReturn(new AuthPojo("jwt-token", "Login exitoso", "maria.caja", "refresh-abc"));

        mockMvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON).content(VALID_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.username").value("maria.caja"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-abc"));
    }

    @Test
    void login_returns401_withSpanishMessage_whenBadCredentials() throws Exception {
        when(authService.login(any(), any())).thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON).content(VALID_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Usuario o contraseña incorrectos"));
    }

    @Test
    void login_returns401_whenUserDisabled() throws Exception {
        when(authService.login(any(), any())).thenThrow(new DisabledException("disabled"));

        mockMvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON).content(VALID_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Usuario deshabilitado"));
    }

    @Test
    void login_returns429_whenTooManyAttempts() throws Exception {
        when(authService.login(any(), any())).thenThrow(new TooManyLoginAttemptsException("Demasiados intentos"));

        mockMvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON).content(VALID_JSON))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void login_returns400_whenBodyEmpty() throws Exception {
        mockMvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void login_returns400_whenUsernameIsNotString() throws Exception {
        mockMvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"username\": 123, \"password\": \"eA==\" }"))
                .andExpect(status().isBadRequest());
    }
}
