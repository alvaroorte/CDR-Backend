package bo.cdr.app.appcatalog.presentation.controller;

import bo.cdr.app.appcatalog.domain.service.interfaces.ProductService;
import bo.cdr.app.appcatalog.presentation.request.dto.ProductAvailabilityDto;
import bo.cdr.app.appcatalog.presentation.request.dto.ProductDto;
import bo.cdr.app.appcatalog.presentation.response.pojo.ProductPojo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Devuelve ProductPojo (no la entidad) porque Product tiene relaciones perezosas
 * (categoría, adicionales) — ver skill backend-crud §6.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/product")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<List<ProductPojo>> getAll() {
        return ResponseEntity.status(HttpStatus.OK).body(productService.getAllProductPojo());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductPojo> getById(@PathVariable Integer id) {
        return ResponseEntity.status(HttpStatus.OK).body(productService.getProductPojoById(id));
    }

    @PostMapping
    public ResponseEntity<ProductPojo> create(@Valid @RequestBody ProductDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductPojo> update(@PathVariable Integer id, @Valid @RequestBody ProductDto dto) {
        return ResponseEntity.status(HttpStatus.OK).body(productService.update(id, dto));
    }

    /** Agotado / disponible. Único endpoint de escritura de productos permitido al CAJERO. */
    @PatchMapping("/{id}/availability")
    public ResponseEntity<ProductPojo> changeAvailability(@PathVariable Integer id,
                                                          @Valid @RequestBody ProductAvailabilityDto dto) {
        return ResponseEntity.status(HttpStatus.OK).body(productService.changeAvailability(id, dto.getAvailable()));
    }

    /** Borrado lógico (desactiva). */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        productService.softDelete(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
