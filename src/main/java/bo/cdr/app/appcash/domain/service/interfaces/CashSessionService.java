package bo.cdr.app.appcash.domain.service.interfaces;

import bo.cdr.app.appbase.domain.service.CRUDService;
import bo.cdr.app.appbase.presentation.response.PagePojo;
import bo.cdr.app.appcash.domain.entity.CashSession;
import bo.cdr.app.appcash.presentation.request.dto.CashCloseDto;
import bo.cdr.app.appcash.presentation.request.dto.CashMovementDto;
import bo.cdr.app.appcash.presentation.request.dto.CashSessionOpenDto;
import bo.cdr.app.appcash.presentation.request.filter.CashSessionFilter;
import bo.cdr.app.appcash.presentation.response.pojo.CashClosePreviewPojo;
import bo.cdr.app.appcash.presentation.response.pojo.CashSessionListPojo;
import bo.cdr.app.appcash.presentation.response.pojo.CashSessionPojo;

import java.util.List;
import java.util.Optional;

public interface CashSessionService extends CRUDService<CashSession, Integer> {

    /** Caja abierta del usuario autenticado con sus totales; vacío si no tiene caja abierta. */
    Optional<CashSessionPojo> getCurrent();

    /** Abre la caja del usuario autenticado (una abierta por usuario) y registra el movimiento OPENING. */
    CashSessionPojo open(CashSessionOpenDto dto);

    /** Ingreso o egreso manual en la caja abierta del usuario autenticado. */
    CashSessionPojo registerMovement(CashMovementDto dto);

    /** Caja abierta del usuario autenticado para registrar dinero (incrementa su versión); 400 "Debe abrir caja" si no tiene. */
    CashSession getOpenSessionOfCurrentUser();

    /** Esperado por medio de pago y pedidos sin entregar, para el arqueo. Dueño o ADMIN. */
    CashClosePreviewPojo getClosePreview(Integer id);

    /** Cierre con arqueo (un declarado por medio). Dueño o ADMIN; si un ADMIN cierra la caja de otro, nota obligatoria. */
    CashSessionPojo close(Integer id, CashCloseDto dto);

    /** Detalle (movimientos y arqueo). Dueño o ADMIN. */
    CashSessionPojo getCashSessionPojoById(Integer id);

    /** Historial: ADMIN todas (filtro por usuario opcional); CAJERO solo las suyas. */
    PagePojo<CashSessionListPojo> pageable(int page, int size, String sortBy, String sortOrder, CashSessionFilter filter);

    /** Cajas abiertas de todos (ADMIN). */
    List<CashSessionListPojo> getOpenSessions();
}
