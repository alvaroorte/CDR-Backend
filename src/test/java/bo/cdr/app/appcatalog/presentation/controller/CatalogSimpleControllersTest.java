package bo.cdr.app.appcatalog.presentation.controller;

import bo.cdr.app.appcatalog.domain.entity.Addon;
import bo.cdr.app.appcatalog.domain.entity.PaymentMethod;
import bo.cdr.app.appcatalog.domain.entity.TableMarker;
import bo.cdr.app.appcatalog.domain.service.interfaces.AddonService;
import bo.cdr.app.appcatalog.domain.service.interfaces.ParameterService;
import bo.cdr.app.appcatalog.domain.service.interfaces.PaymentMethodService;
import bo.cdr.app.appcatalog.domain.service.interfaces.TableMarkerService;
import bo.cdr.app.appcatalog.presentation.request.dto.AddonDto;
import bo.cdr.app.appcatalog.presentation.request.dto.PaymentMethodDto;
import bo.cdr.app.appcatalog.presentation.request.dto.TableMarkerDto;
import bo.cdr.app.appcatalog.presentation.response.pojo.AddonPojo;
import bo.cdr.app.appcatalog.presentation.response.pojo.ParameterPojo;
import bo.cdr.app.appcatalog.presentation.response.pojo.PaymentMethodPojo;
import bo.cdr.app.appcatalog.presentation.response.pojo.TableMarkerPojo;
import bo.cdr.app.appcatalog.presentation.response.pojo.TableMarkerAvailabilityPojo;
import bo.cdr.app.appcatalog.presentation.response.pojo.PosCatalogPojo;
import bo.cdr.app.appcatalog.domain.service.interfaces.PosCatalogService;
import java.math.BigDecimal;
import bo.cdr.app.exception.DuplicateException;
import bo.cdr.app.exception.EntityNotFoundException;
import bo.cdr.app.exception.UpdateNotAllowedException;
import bo.cdr.app.exception.response.ValidationErrorException;
import bo.cdr.app.util.MockMvcTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Controladores de catálogo con contrato CRUD estándar: adicionales, adornos, medios de pago y parámetros. */
@ExtendWith(MockitoExtension.class)
class CatalogSimpleControllersTest {

    @Nested
    class AddonControllerTest {
        @Mock AddonService addonService;
        MockMvc mockMvc;

        @BeforeEach
        void setUp() { mockMvc = MockMvcTestSupport.buildMockMvc(new AddonController(addonService)); }

        @Test
        void getAll_returns200() throws Exception {
            when(addonService.getAllAddonPojo()).thenReturn(List.of(new AddonPojo()));
            mockMvc.perform(get("/addon")).andExpect(status().isOk());
        }

        @Test
        void getSelect_returns200() throws Exception {
            when(addonService.getActive()).thenReturn(List.of());
            mockMvc.perform(get("/addon/select")).andExpect(status().isOk());
        }

        @Test
        void create_returns201() throws Exception {
            when(addonService.create(any(AddonDto.class))).thenReturn(new Addon());
            mockMvc.perform(post("/addon").contentType(MediaType.APPLICATION_JSON)
                    .content("{ \"name\": \"Shot extra\", \"price\": 4, \"active\": true }")).andExpect(status().isCreated());
        }

        @Test
        void create_returns400_whenPriceMissing() throws Exception {
            mockMvc.perform(post("/addon").contentType(MediaType.APPLICATION_JSON)
                    .content("{ \"name\": \"Shot extra\", \"active\": true }")).andExpect(status().isBadRequest());
        }

        @Test
        void delete_returns204() throws Exception {
            mockMvc.perform(delete("/addon/1")).andExpect(status().isNoContent());
            verify(addonService).softDelete(1);
        }
    }

    @Nested
    class TableMarkerControllerTest {
        @Mock TableMarkerService tableMarkerService;
        MockMvc mockMvc;

        @BeforeEach
        void setUp() { mockMvc = MockMvcTestSupport.buildMockMvc(new TableMarkerController(tableMarkerService)); }

        @Test
        void getAll_returns200() throws Exception {
            when(tableMarkerService.getAllTableMarkerPojo()).thenReturn(List.of(new TableMarkerPojo()));
            mockMvc.perform(get("/table-marker")).andExpect(status().isOk());
        }

        @Test
        void create_returns201() throws Exception {
            when(tableMarkerService.create(any(TableMarkerDto.class))).thenReturn(new TableMarker());
            mockMvc.perform(post("/table-marker").contentType(MediaType.APPLICATION_JSON)
                    .content("{ \"number\": 5, \"active\": true }")).andExpect(status().isCreated());
        }

        @Test
        void create_returns400_whenNumberIsZero() throws Exception {
            mockMvc.perform(post("/table-marker").contentType(MediaType.APPLICATION_JSON)
                    .content("{ \"number\": 0, \"active\": true }")).andExpect(status().isBadRequest());
        }

        @Test
        void create_returns409_whenNumberInUse() throws Exception {
            when(tableMarkerService.create(any(TableMarkerDto.class))).thenThrow(new DuplicateException("Número de mesa", "Número", "5"));
            mockMvc.perform(post("/table-marker").contentType(MediaType.APPLICATION_JSON)
                    .content("{ \"number\": 5, \"active\": true }")).andExpect(status().isConflict());
        }

        @Test
        void getById_returns404() throws Exception {
            when(tableMarkerService.getTableMarkerPojoById(8)).thenThrow(new EntityNotFoundException("Número de mesa", 8));
            mockMvc.perform(get("/table-marker/8")).andExpect(status().isNotFound());
        }

        @Test
        void availability_returns200_withDerivedOccupancy() throws Exception {
            when(tableMarkerService.getAvailability()).thenReturn(List.of(
                    new TableMarkerAvailabilityPojo(1, 5, null, true, 40, "#003"),
                    new TableMarkerAvailabilityPojo(2, 6, null, false, null, null)));
            mockMvc.perform(get("/table-marker/availability"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].inUse").value(true))
                    .andExpect(jsonPath("$[0].orderCodeLabel").value("#003"))
                    .andExpect(jsonPath("$[1].inUse").value(false));
        }
    }

    @Nested
    class PosControllerTest {
        @Mock PosCatalogService posCatalogService;
        MockMvc mockMvc;

        @BeforeEach
        void setUp() { mockMvc = MockMvcTestSupport.buildMockMvc(new PosController(posCatalogService)); }

        @Test
        void catalog_returns200() throws Exception {
            when(posCatalogService.getCatalog()).thenReturn(new PosCatalogPojo(List.of(), List.of(),
                    new PosCatalogPojo.DiscountRules(new BigDecimal("50"), BigDecimal.ZERO)));
            mockMvc.perform(get("/pos/catalog"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.discountRules.cashierMaxPercent").value(50));
        }
    }

    @Nested
    class PaymentMethodControllerTest {
        @Mock PaymentMethodService paymentMethodService;
        MockMvc mockMvc;

        private static final String VALID_JSON = """
                { "code": "TARJETA", "name": "Tarjeta", "requiresReference": true, "givesChange": false, "active": true }
                """;

        @BeforeEach
        void setUp() { mockMvc = MockMvcTestSupport.buildMockMvc(new PaymentMethodController(paymentMethodService)); }

        @Test
        void getAll_returns200() throws Exception {
            when(paymentMethodService.getAllPaymentMethodPojo()).thenReturn(List.of(new PaymentMethodPojo()));
            mockMvc.perform(get("/payment-method")).andExpect(status().isOk());
        }

        @Test
        void create_returns201() throws Exception {
            when(paymentMethodService.create(any(PaymentMethodDto.class))).thenReturn(new PaymentMethod());
            mockMvc.perform(post("/payment-method").contentType(MediaType.APPLICATION_JSON).content(VALID_JSON))
                    .andExpect(status().isCreated());
        }

        @Test
        void create_returns400_whenCodeHasSpaces() throws Exception {
            mockMvc.perform(post("/payment-method").contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_JSON.replace("TARJETA", "TAR JETA"))).andExpect(status().isBadRequest());
        }

        @Test
        void update_returns200() throws Exception {
            when(paymentMethodService.update(eq(1), any(PaymentMethodDto.class))).thenReturn(new PaymentMethod());
            mockMvc.perform(put("/payment-method/1").contentType(MediaType.APPLICATION_JSON).content(VALID_JSON))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    class ParameterControllerTest {
        @Mock ParameterService parameterService;
        MockMvc mockMvc;

        @BeforeEach
        void setUp() { mockMvc = MockMvcTestSupport.buildMockMvc(new ParameterController(parameterService)); }

        @Test
        void getAll_returns200() throws Exception {
            when(parameterService.getAllParameterPojo()).thenReturn(List.of(new ParameterPojo()));
            mockMvc.perform(get("/parameter")).andExpect(status().isOk());
        }

        @Test
        void updateValue_returns200() throws Exception {
            when(parameterService.updateValue(1, "40")).thenReturn(new ParameterPojo());
            mockMvc.perform(put("/parameter/1").contentType(MediaType.APPLICATION_JSON).content("{ \"value\": \"40\" }"))
                    .andExpect(status().isOk());
        }

        @Test
        void updateValue_returns400_whenOutOfRange() throws Exception {
            when(parameterService.updateValue(1, "150")).thenThrow(new ValidationErrorException("El porcentaje debe estar entre 0 y 100."));
            mockMvc.perform(put("/parameter/1").contentType(MediaType.APPLICATION_JSON).content("{ \"value\": \"150\" }"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void updateValue_returns400_whenNotEditable() throws Exception {
            when(parameterService.updateValue(2, "x")).thenThrow(new UpdateNotAllowedException("No editable"));
            mockMvc.perform(put("/parameter/2").contentType(MediaType.APPLICATION_JSON).content("{ \"value\": \"x\" }"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void createOrDelete_returns405_becauseParametersComeFromMigrations() throws Exception {
            mockMvc.perform(delete("/parameter/1")).andExpect(status().isMethodNotAllowed());
        }
    }
}
