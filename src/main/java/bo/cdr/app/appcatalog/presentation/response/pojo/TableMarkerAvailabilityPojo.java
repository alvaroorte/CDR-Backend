package bo.cdr.app.appcatalog.presentation.response.pojo;

/**
 * Adorno activo con su ocupación derivada: en uso si tiene un pedido PENDING, IN_PREPARATION o READY.
 * Sin @Pojo: es un record (esa anotación registra un @Component).
 */
public record TableMarkerAvailabilityPojo(Integer id, Integer number, String description, boolean inUse,
                                          Integer orderId, String orderCodeLabel) {
}
