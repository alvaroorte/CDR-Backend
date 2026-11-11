package bo.cdr.app.common.enums;

import bo.cdr.app.exception.InvalidEnumValueException;
import lombok.Getter;

import java.util.Locale;

/** Estados del pedido (skill cdr-order-management §8). En BD se guarda el name(). */
@Getter
public enum OrderStatusEnum {
    PENDING("PENDIENTE"),
    IN_PREPARATION("EN_PREPARACION"),
    READY("LISTO"),
    DELIVERED("ENTREGADO"),
    CANCELLED("CANCELADO");

    /** Estados que mantienen ocupado el adorno de mesa (igual que el índice ux_orders_marker_open). */
    public static final java.util.Set<OrderStatusEnum> OPEN_STATUSES = java.util.EnumSet.of(PENDING, IN_PREPARATION, READY);

    /** Etiqueta en español para la UI. */
    private final String label;

    OrderStatusEnum(String label) {
        this.label = label;
    }

    /** Acepta el name() del enum (lo que viaja en la API). */
    public static OrderStatusEnum fromString(String text) {
        if (text != null) {
            String normalized = text.trim().toUpperCase(Locale.ROOT);
            for (OrderStatusEnum value : values()) {
                if (value.name().equals(normalized)) {
                    return value;
                }
            }
        }
        throw new InvalidEnumValueException("Estado del pedido", text);
    }
}
