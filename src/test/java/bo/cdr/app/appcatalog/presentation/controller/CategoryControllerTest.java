package bo.cdr.app.appcatalog.presentation.controller;

import bo.cdr.app.appbase.presentation.response.SelectPojo;
import bo.cdr.app.appcatalog.domain.entity.Category;
import bo.cdr.app.appcatalog.domain.service.interfaces.CategoryService;
import bo.cdr.app.appcatalog.presentation.request.dto.CategoryDto;
import bo.cdr.app.appcatalog.presentation.response.pojo.CategoryPojo;
import bo.cdr.app.exception.DuplicateException;
import bo.cdr.app.exception.EntityNotFoundException;
import bo.cdr.app.exception.HasAssociatedEntityException;
import bo.cdr.app.util.MockMvcTestSupport;
import org.junit.jupiter.api.BeforeEach;
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

@ExtendWith(MockitoExtension.class)
class CategoryControllerTest {

    @Mock
    CategoryService categoryService;

    MockMvc mockMvc;

    private static final String VALID_JSON = """
            { "name": "Bebidas", "description": "Calientes", "ranking": 1, "active": true }
            """;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcTestSupport.buildMockMvc(new CategoryController(categoryService));
    }

    @Test
    void getAll_returns200() throws Exception {
        when(categoryService.getAllCategoryPojo()).thenReturn(List.of(new CategoryPojo()));
        mockMvc.perform(get("/category")).andExpect(status().isOk()).andExpect(jsonPath("$").isArray());
    }

    @Test
    void getSelect_returns200() throws Exception {
        when(categoryService.getSelect()).thenReturn(List.of(new SelectPojo(1, "Bebidas")));
        mockMvc.perform(get("/category/select")).andExpect(status().isOk()).andExpect(jsonPath("$[0].name").value("Bebidas"));
    }

    @Test
    void getById_returns404_whenNotFound() throws Exception {
        when(categoryService.getCategoryPojoById(99)).thenThrow(new EntityNotFoundException("Categoría", 99));
        mockMvc.perform(get("/category/99")).andExpect(status().isNotFound());
    }

    @Test
    void create_returns201_whenBodyValid() throws Exception {
        when(categoryService.create(any(CategoryDto.class))).thenReturn(new Category());
        mockMvc.perform(post("/category").contentType(MediaType.APPLICATION_JSON).content(VALID_JSON))
                .andExpect(status().isCreated());
    }

    @Test
    void create_returns400_whenRequiredFieldsMissing() throws Exception {
        mockMvc.perform(post("/category").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_returns400_whenImageIsNotAnImage() throws Exception {
        mockMvc.perform(post("/category").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"name\": \"Bebidas\", \"imageBase64\": \"aG9sYQ==\", \"ranking\": 1, \"active\": true }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("imageBase64"));
    }

    @Test
    void create_returns409_whenDuplicate() throws Exception {
        when(categoryService.create(any(CategoryDto.class))).thenThrow(new DuplicateException("Categoría", "Nombre", "Bebidas"));
        mockMvc.perform(post("/category").contentType(MediaType.APPLICATION_JSON).content(VALID_JSON))
                .andExpect(status().isConflict());
    }

    @Test
    void update_returns200_whenBodyValid() throws Exception {
        when(categoryService.update(eq(1), any(CategoryDto.class))).thenReturn(new Category());
        mockMvc.perform(put("/category/1").contentType(MediaType.APPLICATION_JSON).content(VALID_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void delete_returns204() throws Exception {
        doNothing().when(categoryService).softDelete(1);
        mockMvc.perform(delete("/category/1")).andExpect(status().isNoContent());
    }

    @Test
    void delete_returns400_whenCategoryHasActiveProducts() throws Exception {
        doThrow(new HasAssociatedEntityException("Categoría", "productos activos")).when(categoryService).softDelete(1);
        mockMvc.perform(delete("/category/1")).andExpect(status().isBadRequest());
    }
}
