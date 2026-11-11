package bo.cdr.app.appcatalog.presentation.controller;

import bo.cdr.app.appcatalog.domain.entity.Addon;
import bo.cdr.app.appcatalog.domain.service.interfaces.AddonService;
import bo.cdr.app.appcatalog.presentation.request.dto.AddonDto;
import bo.cdr.app.appcatalog.presentation.response.pojo.AddonPojo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/addon")
public class AddonController {

    private final AddonService addonService;

    @GetMapping
    public ResponseEntity<List<AddonPojo>> getAll() {
        return ResponseEntity.status(HttpStatus.OK).body(addonService.getAllAddonPojo());
    }

    /** Solo activos (para el formulario de productos). */
    @GetMapping("/select")
    public ResponseEntity<List<AddonPojo>> getSelect() {
        return ResponseEntity.status(HttpStatus.OK).body(addonService.getActive());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AddonPojo> getById(@PathVariable Integer id) {
        return ResponseEntity.status(HttpStatus.OK).body(addonService.getAddonPojoById(id));
    }

    @PostMapping
    public ResponseEntity<Addon> create(@Valid @RequestBody AddonDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(addonService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Addon> update(@PathVariable Integer id, @Valid @RequestBody AddonDto dto) {
        return ResponseEntity.status(HttpStatus.OK).body(addonService.update(id, dto));
    }

    /** Borrado lógico (desactiva). */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        addonService.softDelete(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
