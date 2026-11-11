package bo.cdr.app.appcash.presentation.controller;

import bo.cdr.app.appbase.presentation.response.PagePojo;
import bo.cdr.app.appcash.domain.service.interfaces.CashSessionService;
import bo.cdr.app.appcash.presentation.request.dto.CashCloseDto;
import bo.cdr.app.appcash.presentation.request.dto.CashMovementDto;
import bo.cdr.app.appcash.presentation.request.dto.CashSessionOpenDto;
import bo.cdr.app.appcash.presentation.request.filter.CashSessionFilter;
import bo.cdr.app.appcash.presentation.response.pojo.CashClosePreviewPojo;
import bo.cdr.app.appcash.presentation.response.pojo.CashSessionListPojo;
import bo.cdr.app.appcash.presentation.response.pojo.CashSessionPojo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Caja: apertura y movimientos del usuario (Etapa 3); arqueo, cierre e historial (Etapa 5).
 * Quién puede ver o cerrar cada caja lo decide el servicio (dueño o ADMIN).
 */
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

    /** Historial: ADMIN todas (filtro userId opcional); CAJERO solo las suyas. */
    @GetMapping("/pageable")
    public ResponseEntity<PagePojo<CashSessionListPojo>> pageable(@RequestParam(defaultValue = "0") int page,
                                                                   @RequestParam(defaultValue = "10") int size,
                                                                   @RequestParam(required = false) String sortBy,
                                                                   @RequestParam(required = false) String sortOrder,
                                                                   @ModelAttribute CashSessionFilter filter) {
        return ResponseEntity.status(HttpStatus.OK).body(cashSessionService.pageable(page, size, sortBy, sortOrder, filter));
    }

    /** Cajas abiertas de todos los usuarios (solo ADMIN: no tiene permiso para otros roles). */
    @GetMapping("/open-sessions")
    public ResponseEntity<List<CashSessionListPojo>> getOpenSessions() {
        return ResponseEntity.status(HttpStatus.OK).body(cashSessionService.getOpenSessions());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CashSessionPojo> getById(@PathVariable Integer id) {
        return ResponseEntity.status(HttpStatus.OK).body(cashSessionService.getCashSessionPojoById(id));
    }

    @GetMapping("/{id}/close-preview")
    public ResponseEntity<CashClosePreviewPojo> getClosePreview(@PathVariable Integer id) {
        return ResponseEntity.status(HttpStatus.OK).body(cashSessionService.getClosePreview(id));
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<CashSessionPojo> close(@PathVariable Integer id, @Valid @RequestBody CashCloseDto dto) {
        return ResponseEntity.status(HttpStatus.OK).body(cashSessionService.close(id, dto));
    }
}
