package bo.cdr.app.apporder.presentation.response.pojo;

import bo.cdr.app.common.enums.OrderStatusEnum;

import java.time.LocalDateTime;

/** Línea de tiempo de estados del pedido. Record sin @Pojo. */
public record OrderStatusHistoryPojo(OrderStatusEnum fromStatus,
                                     String fromStatusLabel,
                                     OrderStatusEnum toStatus,
                                     String toStatusLabel,
                                     String changedBy,
                                     LocalDateTime changedAt,
                                     String note) {
}
