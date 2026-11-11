package bo.cdr.app.appcash.presentation.controller;

import bo.cdr.app.appcash.domain.service.interfaces.CashSessionService;
import bo.cdr.app.appcash.presentation.request.dto.CashMovementDto;
import bo.cdr.app.appcash.presentation.request.dto.CashSessionOpenDto;
import bo.cdr.app.appcash.presentation.response.pojo.CashSessionPojo;
import bo.cdr.app.exception.ConflictException;
import bo.cdr.app.util.MockMvcTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CashSessionControllerTest {

    @Mock CashSessionService cashSessionService;
    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcTestSupport.buildMockMvc(new CashSessionController(cashSessionService));
    }

    @Test
    void getCurrent_returns200_whenOpen() throws Exception {
        CashSessionPojo pojo = new CashSessionPojo();
        pojo.setId(3);
        when(cashSessionService.getCurrent()).thenReturn(Optional.of(pojo));
        mockMvc.perform(get("/cash-session/current")).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(3));
    }

    @Test
    void getCurrent_returns204_whenNoOpenSession() throws Exception {
        when(cashSessionService.getCurrent()).thenReturn(Optional.empty());
        mockMvc.perform(get("/cash-session/current")).andExpect(status().isNoContent());
    }

    @Test
    void open_returns201() throws Exception {
        when(cashSessionService.open(any(CashSessionOpenDto.class))).thenReturn(new CashSessionPojo());
        mockMvc.perform(post("/cash-session/open").contentType(MediaType.APPLICATION_JSON).content("{ \"openingAmount\": 100.50 }"))
                .andExpect(status().isCreated());
    }

    @Test
    void open_returns400_whenAmountNegative() throws Exception {
        mockMvc.perform(post("/cash-session/open").contentType(MediaType.APPLICATION_JSON).content("{ \"openingAmount\": -1 }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("openingAmount"));
    }

    @Test
    void open_returns409_whenAlreadyOpen() throws Exception {
        when(cashSessionService.open(any())).thenThrow(new ConflictException("Ya tiene una caja abierta."));
        mockMvc.perform(post("/cash-session/open").contentType(MediaType.APPLICATION_JSON).content("{ \"openingAmount\": 0 }"))
                .andExpect(status().isConflict());
    }

    @Test
    void movement_returns201() throws Exception {
        when(cashSessionService.registerMovement(any(CashMovementDto.class))).thenReturn(new CashSessionPojo());
        mockMvc.perform(post("/cash-session/movement").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"type\": \"EXPENSE\", \"amount\": 4.50, \"description\": \"Hielo\" }"))
                .andExpect(status().isCreated());
    }

    @Test
    void movement_returns400_whenDescriptionMissing() throws Exception {
        mockMvc.perform(post("/cash-session/movement").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"type\": \"EXPENSE\", \"amount\": 4.50 }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("description"));
        verifyNoInteractions(cashSessionService);
    }

    // ----- Etapa 5 -----

    @Test
    void closePreview_returns200() throws Exception {
        when(cashSessionService.getClosePreview(3)).thenReturn(new bo.cdr.app.appcash.presentation.response.pojo.CashClosePreviewPojo(
                3, 7L, 4, "María", true, null, java.math.BigDecimal.TEN, java.util.List.of(), java.util.List.of()));
        mockMvc.perform(get("/cash-session/3/close-preview")).andExpect(status().isOk()).andExpect(jsonPath("$.version").value(7));
    }

    @Test
    void close_returns200() throws Exception {
        when(cashSessionService.close(org.mockito.ArgumentMatchers.eq(3), any(bo.cdr.app.appcash.presentation.request.dto.CashCloseDto.class)))
                .thenReturn(new CashSessionPojo());
        mockMvc.perform(post("/cash-session/3/close").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"expectedVersion\": 7, \"declared\": [ { \"paymentMethodId\": 1, \"amount\": 120.00 } ] }"))
                .andExpect(status().isOk());
    }

    @Test
    void close_returns400_whenDeclaredMissingOrNegative() throws Exception {
        mockMvc.perform(post("/cash-session/3/close").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"expectedVersion\": 7, \"declared\": [] }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("declared"));
        mockMvc.perform(post("/cash-session/3/close").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"expectedVersion\": 7, \"declared\": [ { \"paymentMethodId\": 1, \"amount\": -1 } ] }"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(cashSessionService);
    }

    @Test
    void close_returns409_whenStale() throws Exception {
        when(cashSessionService.close(org.mockito.ArgumentMatchers.eq(3), any(bo.cdr.app.appcash.presentation.request.dto.CashCloseDto.class)))
                .thenThrow(new ConflictException("La caja tuvo movimientos"));
        mockMvc.perform(post("/cash-session/3/close").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"expectedVersion\": 6, \"declared\": [ { \"paymentMethodId\": 1, \"amount\": 1 } ] }"))
                .andExpect(status().isConflict());
    }

    @Test
    void pageable_and_openSessions_return200() throws Exception {
        when(cashSessionService.pageable(org.mockito.ArgumentMatchers.eq(0), org.mockito.ArgumentMatchers.eq(10),
                org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.isNull(), any()))
                .thenReturn(new bo.cdr.app.appbase.presentation.response.PagePojo<>());
        mockMvc.perform(get("/cash-session/pageable").param("status", "CLOSED")).andExpect(status().isOk());
        when(cashSessionService.getOpenSessions()).thenReturn(java.util.List.of());
        mockMvc.perform(get("/cash-session/open-sessions")).andExpect(status().isOk());
    }
}
