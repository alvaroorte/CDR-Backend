package bo.cdr.app.appsecurity.presentation.controller;

import bo.cdr.app.appsecurity.auth.service.AuthService;
import bo.cdr.app.appsecurity.presentation.response.pojo.AuthPojo;
import bo.cdr.app.exception.UnauthorizedException;
import bo.cdr.app.util.MockMvcTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthTokenControllerTest {

    @Mock
    AuthService authService;

    MockMvc mockMvc;
    final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcTestSupport.buildMockMvc(new AuthTokenController(authService));
    }

    @Test
    void refresh_returnsOk_withNewTokens() throws Exception {
        AuthPojo response = new AuthPojo("new-jwt", "Token renovado", "juan.perez", "new-refresh");
        when(authService.refresh(any())).thenReturn(response);

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"old-refresh\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("new-jwt"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh"))
                .andExpect(jsonPath("$.username").value("juan.perez"));
    }

    @Test
    void refresh_returns401_whenTokenInvalid() throws Exception {
        when(authService.refresh(any())).thenThrow(new UnauthorizedException("Refresh token inválido"));

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"bad\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_returns401_whenUserInactive() throws Exception {
        when(authService.refresh(any())).thenThrow(new UnauthorizedException("Usuario inactivo o no encontrado"));

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"valid-but-inactive\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_returns400_whenBodyBlank() throws Exception {
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void logout_returnsOk_andRevokes() throws Exception {
        mockMvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"some-token\"}"))
                .andExpect(status().isOk());

        verify(authService).logout("some-token");
    }
}
