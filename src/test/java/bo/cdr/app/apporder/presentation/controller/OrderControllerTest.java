package bo.cdr.app.apporder.presentation.controller;

import bo.cdr.app.apporder.domain.service.interfaces.OrderService;
import bo.cdr.app.apporder.presentation.request.dto.OrderDto;
import bo.cdr.app.apporder.presentation.request.dto.OrderQuoteDto;
import bo.cdr.app.apporder.presentation.response.pojo.OrderPojo;
import bo.cdr.app.apporder.presentation.response.pojo.OrderQuotePojo;
import bo.cdr.app.exception.ConflictException;
import bo.cdr.app.exception.EntityNotFoundException;
import bo.cdr.app.exception.NotAllowedException;
import bo.cdr.app.exception.response.ValidationErrorException;
import bo.cdr.app.util.MockMvcTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock OrderService orderService;
    MockMvc mockMvc;

    private static final String QUOTE_JSON = """
            { "items": [ { "productId": 10, "quantity": 2, "addonIds": [5], "notes": "sin azúcar" } ],
              "discount": { "type": "PERCENTAGE", "value": 10, "reason": "Cliente frecuente" } }
            """;

    private static final String ORDER_JSON = """
            { "items": [ { "productId": 10, "quantity": 1 } ],
              "serviceType": "DINE_IN", "tableMarkerId": 7,
              "payments": [ { "paymentMethodId": 1, "amount": 15.00 } ] }
            """;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcTestSupport.buildMockMvc(new OrderController(orderService));
    }

    @Test
    void quote_returns200_withBackendTotals() throws Exception {
        OrderQuotePojo pojo = new OrderQuotePojo();
        pojo.setTotal(new BigDecimal("33.30"));
        when(orderService.quote(any(OrderQuoteDto.class))).thenReturn(pojo);

        mockMvc.perform(post("/order/quote").contentType(MediaType.APPLICATION_JSON).content(QUOTE_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(33.30));
    }

    @Test
    void quote_returns400_whenCartIsEmpty() throws Exception {
        mockMvc.perform(post("/order/quote").contentType(MediaType.APPLICATION_JSON).content("{ \"items\": [] }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("items"));
        verifyNoInteractions(orderService);
    }

    @Test
    void quote_returns400_whenQuantityIsZero() throws Exception {
        mockMvc.perform(post("/order/quote").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"items\": [ { \"productId\": 10, \"quantity\": 0 } ] }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("items[0].quantity"));
    }

    @Test
    void quote_returns400_whenDiscountTypeInvalid() throws Exception {
        mockMvc.perform(post("/order/quote").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"items\": [ { \"productId\": 10, \"quantity\": 1 } ], \"discount\": { \"type\": \"GRATIS\", \"value\": 5 } }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("discount.type"));
    }

    @Test
    void quote_returns400_whenCashierExceedsDiscountLimit() throws Exception {
        when(orderService.quote(any())).thenThrow(new NotAllowedException("El descuento supera el máximo permitido al cajero."));
        mockMvc.perform(post("/order/quote").contentType(MediaType.APPLICATION_JSON).content(QUOTE_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El descuento supera el máximo permitido al cajero."));
    }

    @Test
    void create_returns201() throws Exception {
        OrderPojo pojo = new OrderPojo();
        pojo.setCodeLabel("#003");
        when(orderService.create(any(OrderDto.class))).thenReturn(pojo);

        mockMvc.perform(post("/order").contentType(MediaType.APPLICATION_JSON).content(ORDER_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codeLabel").value("#003"));
    }

    @Test
    void create_returns400_whenNoPayments() throws Exception {
        mockMvc.perform(post("/order").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"items\": [ { \"productId\": 10, \"quantity\": 1 } ], \"serviceType\": \"TAKEAWAY\", \"payments\": [] }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("payments"));
    }

    @Test
    void create_returns400_whenServiceTypeInvalid() throws Exception {
        mockMvc.perform(post("/order").contentType(MediaType.APPLICATION_JSON)
                        .content(ORDER_JSON.replace("DINE_IN", "DELIVERY")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("serviceType"));
    }

    @Test
    void create_returns400_whenCashSessionClosed() throws Exception {
        when(orderService.create(any(OrderDto.class))).thenThrow(new ValidationErrorException("Debe abrir caja antes de registrar cobros o movimientos."));
        mockMvc.perform(post("/order").contentType(MediaType.APPLICATION_JSON).content(ORDER_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_returns409_whenMarkerInUse() throws Exception {
        when(orderService.create(any(OrderDto.class))).thenThrow(new ConflictException("El número de mesa 5 está en uso. Elija otro."));
        mockMvc.perform(post("/order").contentType(MediaType.APPLICATION_JSON).content(ORDER_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("El número de mesa 5 está en uso. Elija otro."));
    }

    @Test
    void getById_returns404_whenNotFound() throws Exception {
        when(orderService.getOrderPojoById(9)).thenThrow(new EntityNotFoundException("Pedido", 9));
        mockMvc.perform(get("/order/9")).andExpect(status().isNotFound());
    }

    // ----- Etapa 4 -----

    @Test
    void board_returns200_andParsesStatuses() throws Exception {
        when(orderService.getBoard(java.util.List.of(bo.cdr.app.common.enums.OrderStatusEnum.READY))).thenReturn(java.util.List.of());
        mockMvc.perform(get("/order/board").param("statuses", "READY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void changeStatus_returns200() throws Exception {
        when(orderService.changeStatus(org.mockito.ArgumentMatchers.eq(40), any(bo.cdr.app.apporder.presentation.request.dto.OrderStatusDto.class)))
                .thenReturn(new OrderPojo());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/order/40/status")
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"toStatus\": \"IN_PREPARATION\", \"expectedVersion\": 2 }"))
                .andExpect(status().isOk());
    }

    @Test
    void changeStatus_returns400_whenVersionMissing() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/order/40/status")
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"toStatus\": \"READY\" }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("expectedVersion"));
    }

    @Test
    void changeStatus_returns409_whenStale() throws Exception {
        when(orderService.changeStatus(org.mockito.ArgumentMatchers.eq(40), any(bo.cdr.app.apporder.presentation.request.dto.OrderStatusDto.class)))
                .thenThrow(new ConflictException("El pedido #003 fue modificado por otro usuario."));
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/order/40/status")
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"toStatus\": \"READY\", \"expectedVersion\": 1 }"))
                .andExpect(status().isConflict());
    }

    @Test
    void cancel_returns400_whenReasonBlank() throws Exception {
        mockMvc.perform(post("/order/40/cancel").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"reason\": \"  \", \"expectedVersion\": 2 }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("reason"));
    }

    @Test
    void cancel_returns200() throws Exception {
        when(orderService.cancel(org.mockito.ArgumentMatchers.eq(40), any(bo.cdr.app.apporder.presentation.request.dto.OrderCancelDto.class)))
                .thenReturn(new OrderPojo());
        mockMvc.perform(post("/order/40/cancel").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"reason\": \"Cliente se retiró\", \"expectedVersion\": 2 }"))
                .andExpect(status().isOk());
    }

    @Test
    void pageable_bindsFilter() throws Exception {
        when(orderService.pageable(org.mockito.ArgumentMatchers.eq(0), org.mockito.ArgumentMatchers.eq(10),
                org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.argThat(f -> f.getDateFrom() != null && "READY".equals(f.getStatus()))))
                .thenReturn(new bo.cdr.app.appbase.presentation.response.PagePojo<>());
        mockMvc.perform(get("/order/pageable").param("dateFrom", "2026-11-01").param("status", "READY"))
                .andExpect(status().isOk());
    }

    @Test
    void kitchenSettings_returns200() throws Exception {
        when(orderService.getKitchenSettings()).thenReturn(new bo.cdr.app.apporder.presentation.response.pojo.KitchenSettingsPojo(10, 20));
        mockMvc.perform(get("/order/kitchen-settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.delayCriticalMinutes").value(20));
    }
}
