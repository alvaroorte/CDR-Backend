package bo.cdr.app.apporder.presentation.response.pojo;

import bo.cdr.app.common.enums.OrderEventTypeEnum;
import bo.cdr.app.common.enums.OrderStatusEnum;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Mensaje de /topic/orders. El cliente descarta eventos con {@code version} menor o igual a la que
 * ya tiene para ese pedido (duplicados o desordenados).
 */
public record OrderEventPojo(String eventId,
                             OrderEventTypeEnum type,
                             Integer orderId,
                             Long version,
                             OrderStatusEnum status,
                             LocalDateTime occurredAt,
                             OrderBoardPojo order) {

    public static OrderEventPojo of(OrderEventTypeEnum type, OrderBoardPojo order, LocalDateTime occurredAt) {
        return new OrderEventPojo(UUID.randomUUID().toString(), type, order.id(), order.version(), order.status(),
                occurredAt, order);
    }
}
