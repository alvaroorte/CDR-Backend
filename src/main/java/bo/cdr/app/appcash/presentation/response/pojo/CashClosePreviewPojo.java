package bo.cdr.app.appcash.presentation.response.pojo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Datos para el arqueo antes de cerrar: esperado por medio (medios activos y los que tuvieron
 * movimientos) y pedidos de esta caja sin entregar (se permite cerrar con advertencia).
 */
public record CashClosePreviewPojo(Integer cashSessionId,
                                   Long version,
                                   Integer userId,
                                   String userFullName,
                                   boolean ownSession,
                                   LocalDateTime openedAt,
                                   BigDecimal openingAmount,
                                   List<CashMethodExpectedPojo> methods,
                                   List<CashOpenOrderPojo> openOrders) {
}
