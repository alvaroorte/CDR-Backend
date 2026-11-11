package bo.cdr.app.appcatalog.presentation.response.pojo;

/** Fila de la consulta de ocupación: adorno + pedido abierto (null si está libre). */
public record TableMarkerOccupancyProjection(Integer id, Integer number, String description, Integer orderId,
                                             Integer orderCode) {
}
