package bo.cdr.app.apporder.domain.event;

import bo.cdr.app.common.enums.OrderEventTypeEnum;

/**
 * Evento de aplicación publicado DENTRO de la transacción; OrderEventPublisher lo envía por
 * WebSocket solo después del commit (nunca se emite algo que hizo rollback).
 */
public record OrderChangedEvent(Integer orderId, OrderEventTypeEnum type) {
}
