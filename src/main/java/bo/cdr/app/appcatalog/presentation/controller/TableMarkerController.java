package bo.cdr.app.appcatalog.presentation.controller;

import bo.cdr.app.appcatalog.domain.entity.TableMarker;
import bo.cdr.app.appcatalog.domain.service.interfaces.TableMarkerService;
import bo.cdr.app.appcatalog.presentation.request.dto.TableMarkerDto;
import bo.cdr.app.appcatalog.presentation.response.pojo.TableMarkerAvailabilityPojo;
import bo.cdr.app.appcatalog.presentation.response.pojo.TableMarkerPojo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/table-marker")
public class TableMarkerController {

    private final TableMarkerService tableMarkerService;

    @GetMapping
    public ResponseEntity<List<TableMarkerPojo>> getAll() {
        return ResponseEntity.status(HttpStatus.OK).body(tableMarkerService.getAllTableMarkerPojo());
    }

    /** Números activos con ocupación (en uso mientras tengan un pedido abierto). Para el POS. */
    @GetMapping("/availability")
    public ResponseEntity<List<TableMarkerAvailabilityPojo>> getAvailability() {
        return ResponseEntity.status(HttpStatus.OK).body(tableMarkerService.getAvailability());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TableMarkerPojo> getById(@PathVariable Integer id) {
        return ResponseEntity.status(HttpStatus.OK).body(tableMarkerService.getTableMarkerPojoById(id));
    }

    @PostMapping
    public ResponseEntity<TableMarker> create(@Valid @RequestBody TableMarkerDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tableMarkerService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TableMarker> update(@PathVariable Integer id, @Valid @RequestBody TableMarkerDto dto) {
        return ResponseEntity.status(HttpStatus.OK).body(tableMarkerService.update(id, dto));
    }

    /** Borrado lógico (desactiva). */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        tableMarkerService.softDelete(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
