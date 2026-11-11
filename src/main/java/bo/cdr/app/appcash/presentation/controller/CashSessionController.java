package bo.cdr.app.appcash.presentation.controller;

import bo.cdr.app.appcash.domain.service.interfaces.CashSessionService;
import bo.cdr.app.appcash.presentation.request.dto.CashMovementDto;
import bo.cdr.app.appcash.presentation.request.dto.CashSessionOpenDto;
import bo.cdr.app.appcash.presentation.response.pojo.CashSessionPojo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Caja del usuario autenticado. Etapa 3: consulta, apertura y movimientos manuales. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/cash-session")
public class CashSessionController {

    private final CashSessionService cashSessionService;

    /** 200 con la caja abierta; 204 si el usuario no tiene caja abierta. */
    @GetMapping("/current")
    public ResponseEntity<CashSessionPojo> getCurrent() {
        return cashSessionService.getCurrent()
                .map(session -> ResponseEntity.status(HttpStatus.OK).body(session))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NO_CONTENT).build());
    }

    @PostMapping("/open")
    public ResponseEntity<CashSessionPojo> open(@Valid @RequestBody CashSessionOpenDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cashSessionService.open(dto));
    }

    @PostMapping("/movement")
    public ResponseEntity<CashSessionPojo> registerMovement(@Valid @RequestBody CashMovementDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cashSessionService.registerMovement(dto));
    }
}
