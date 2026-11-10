package bo.cdr.app.exception;

import bo.cdr.app.util.MockMvcTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ErrorHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcTestSupport.buildMockMvc(new ThrowingController());
    }

    @Test
    void genericException_returns500WithErrorResponse() throws Exception {
        mockMvc.perform(get("/test/generic"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("Error interno del servidor"))
                .andExpect(jsonPath("$.path").value("/test/generic"));
    }

    @Test
    void entityNotFoundException_returns404WithErrorResponse() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("no existe"));
    }

    /**
     * Bloquea el fix: el @ControllerAdvice debe estar acotado a bo.cdr.app
     * para no interceptar controladores internos de Spring (p. ej. BasicErrorController)
     * durante el manejo de errores del transporte SockJS.
     */
    @Test
    void controllerAdvice_isScopedToProjectBasePackage() {
        ControllerAdvice advice = ErrorHandler.class.getAnnotation(ControllerAdvice.class);
        assertTrue(Arrays.asList(advice.basePackages()).contains("bo.cdr.app"),
                "ErrorHandler debe estar acotado con @ControllerAdvice(basePackages=\"bo.cdr.app\")");
    }

    @RestController
    static class ThrowingController {

        @GetMapping("/test/generic")
        String generic() {
            throw new RuntimeException("boom");
        }

        @GetMapping("/test/not-found")
        String notFound() {
            throw new EntityNotFoundException("no existe");
        }
    }
}
