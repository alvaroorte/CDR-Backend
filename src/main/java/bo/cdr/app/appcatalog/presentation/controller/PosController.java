package bo.cdr.app.appcatalog.presentation.controller;

import bo.cdr.app.appcatalog.domain.service.interfaces.PosCatalogService;
import bo.cdr.app.appcatalog.presentation.response.pojo.PosCatalogPojo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/pos")
public class PosController {

    private final PosCatalogService posCatalogService;

    @GetMapping("/catalog")
    public ResponseEntity<PosCatalogPojo> getCatalog() {
        return ResponseEntity.status(HttpStatus.OK).body(posCatalogService.getCatalog());
    }
}
