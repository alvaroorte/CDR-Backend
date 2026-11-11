package bo.cdr.app.common.enums;

/** Tipos de evento publicados en /topic/orders (docs/architecture/realtime.md §3). */
public enum OrderEventTypeEnum {
    ORDER_CREATED,
    ORDER_STATUS_CHANGED,
    ORDER_CANCELLED
}
