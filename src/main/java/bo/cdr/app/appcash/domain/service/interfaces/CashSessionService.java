package bo.cdr.app.appcash.domain.service.interfaces;

import bo.cdr.app.appbase.domain.service.CRUDService;
import bo.cdr.app.appcash.domain.entity.CashSession;
import bo.cdr.app.appcash.presentation.request.dto.CashMovementDto;
import bo.cdr.app.appcash.presentation.request.dto.CashSessionOpenDto;
import bo.cdr.app.appcash.presentation.response.pojo.CashSessionPojo;

import java.util.Optional;

public interface CashSessionService extends CRUDService<CashSession, Integer> {

    /** Caja abierta del usuario autenticado con sus totales; vacío si no tiene caja abierta. */
    Optional<CashSessionPojo> getCurrent();

    /** Abre la caja del usuario autenticado (una abierta por usuario) y registra el movimiento OPENING. */
    CashSessionPojo open(CashSessionOpenDto dto);

    /** Ingreso o egreso manual en la caja abierta del usuario autenticado. */
    CashSessionPojo registerMovement(CashMovementDto dto);

    /** Caja abierta del usuario autenticado; 400 "Debe abrir caja" si no tiene. */
    CashSession getOpenSessionOfCurrentUser();
}
