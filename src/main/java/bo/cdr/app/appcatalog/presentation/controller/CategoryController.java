package bo.cdr.app.appcatalog.presentation.controller;

import bo.cdr.app.appbase.presentation.response.SelectPojo;
import bo.cdr.app.appcatalog.domain.entity.Category;
import bo.cdr.app.appcatalog.domain.service.interfaces.CategoryService;
import bo.cdr.app.appcatalog.presentation.request.dto.CategoryDto;
import bo.cdr.app.appcatalog.presentation.response.pojo.CategoryPojo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/category")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<List<CategoryPojo>> getAll() {
        return ResponseEntity.status(HttpStatus.OK).body(categoryService.getAllCategoryPojo());
    }

    @GetMapping("/select")
    public ResponseEntity<List<SelectPojo>> getSelect() {
        return ResponseEntity.status(HttpStatus.OK).body(categoryService.getSelect());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryPojo> getById(@PathVariable Integer id) {
        return ResponseEntity.status(HttpStatus.OK).body(categoryService.getCategoryPojoById(id));
    }

    @PostMapping
    public ResponseEntity<Category> create(@Valid @RequestBody CategoryDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Category> update(@PathVariable Integer id, @Valid @RequestBody CategoryDto dto) {
        return ResponseEntity.status(HttpStatus.OK).body(categoryService.update(id, dto));
    }

    /** Borrado lógico (desactiva). Bloqueado si la categoría tiene productos activos. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        categoryService.softDelete(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
