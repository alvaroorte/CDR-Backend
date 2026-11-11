package bo.cdr.app.appcatalog.presentation.controller;

import bo.cdr.app.appcatalog.domain.service.interfaces.ParameterService;
import bo.cdr.app.appcatalog.presentation.request.dto.ParameterValueDto;
import bo.cdr.app.appcatalog.presentation.response.pojo.ParameterPojo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Sin POST/DELETE: los parámetros los define una migración porque el código depende de sus códigos. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/parameter")
public class ParameterController {

    private final ParameterService parameterService;

    @GetMapping
    public ResponseEntity<List<ParameterPojo>> getAll() {
        return ResponseEntity.status(HttpStatus.OK).body(parameterService.getAllParameterPojo());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ParameterPojo> getById(@PathVariable Integer id) {
        return ResponseEntity.status(HttpStatus.OK).body(parameterService.getParameterPojoById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ParameterPojo> updateValue(@PathVariable Integer id, @Valid @RequestBody ParameterValueDto dto) {
        return ResponseEntity.status(HttpStatus.OK).body(parameterService.updateValue(id, dto.getValue()));
    }
}
