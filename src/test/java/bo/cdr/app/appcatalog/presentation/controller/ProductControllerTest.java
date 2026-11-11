package bo.cdr.app.appcatalog.presentation.controller;

import bo.cdr.app.appcatalog.domain.service.interfaces.ProductService;
import bo.cdr.app.appcatalog.presentation.request.dto.ProductDto;
import bo.cdr.app.appcatalog.presentation.response.pojo.ProductPojo;
import bo.cdr.app.exception.DuplicateException;
import bo.cdr.app.exception.EntityNotFoundException;
import bo.cdr.app.util.MockMvcTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    @Mock
    ProductService productService;

    MockMvc mockMvc;

    private static final String VALID_JSON = """
            { "categoryId": 1, "name": "Capuchino", "price": 15.50, "available": true, "active": true, "addonIds": [2, 3] }
            """;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcTestSupport.buildMockMvc(new ProductController(productService));
    }

    @Test
    void getAll_returns200() throws Exception {
        when(productService.getAllProductPojo()).thenReturn(List.of(new ProductPojo()));
        mockMvc.perform(get("/product")).andExpect(status().isOk()).andExpect(jsonPath("$").isArray());
    }

    @Test
    void getById_returns404_whenNotFound() throws Exception {
        when(productService.getProductPojoById(9)).thenThrow(new EntityNotFoundException("Producto", 9));
        mockMvc.perform(get("/product/9")).andExpect(status().isNotFound());
    }

    @Test
    void create_returns201_whenBodyValid() throws Exception {
        when(productService.create(any(ProductDto.class))).thenReturn(new ProductPojo());
        mockMvc.perform(post("/product").contentType(MediaType.APPLICATION_JSON).content(VALID_JSON))
                .andExpect(status().isCreated());
    }

    @Test
    void create_returns400_whenPriceHasThreeDecimals() throws Exception {
        mockMvc.perform(post("/product").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"categoryId\": 1, \"name\": \"Capuchino\", \"price\": 15.555, \"available\": true, \"active\": true }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("price"));
    }

    @Test
    void create_returns400_whenPriceIsNegative() throws Exception {
        mockMvc.perform(post("/product").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"categoryId\": 1, \"name\": \"Capuchino\", \"price\": -1, \"available\": true, \"active\": true }"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_returns400_whenAddonIdsAreNotIntegers() throws Exception {
        mockMvc.perform(post("/product").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"categoryId\": 1, \"name\": \"Capuchino\", \"price\": 15, \"available\": true, \"active\": true, \"addonIds\": [\"a\"] }"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_returns409_whenDuplicate() throws Exception {
        when(productService.create(any(ProductDto.class))).thenThrow(new DuplicateException("Producto", "Nombre", "Capuchino"));
        mockMvc.perform(post("/product").contentType(MediaType.APPLICATION_JSON).content(VALID_JSON))
                .andExpect(status().isConflict());
    }

    @Test
    void update_returns409_whenConcurrentModification() throws Exception {
        when(productService.update(eq(1), any(ProductDto.class)))
                .thenThrow(new ObjectOptimisticLockingFailureException("Product", 1));
        mockMvc.perform(put("/product/1").contentType(MediaType.APPLICATION_JSON).content(VALID_JSON))
                .andExpect(status().isConflict());
    }

    @Test
    void changeAvailability_returns200() throws Exception {
        ProductPojo pojo = new ProductPojo();
        pojo.setAvailable(false);
        pojo.setPrice(new BigDecimal("15.50"));
        when(productService.changeAvailability(1, false)).thenReturn(pojo);
        mockMvc.perform(patch("/product/1/availability").contentType(MediaType.APPLICATION_JSON).content("{ \"available\": false }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false));
    }

    @Test
    void changeAvailability_returns400_whenAvailableMissing() throws Exception {
        mockMvc.perform(patch("/product/1/availability").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void delete_returns204() throws Exception {
        doNothing().when(productService).softDelete(1);
        mockMvc.perform(delete("/product/1")).andExpect(status().isNoContent());
    }
}
