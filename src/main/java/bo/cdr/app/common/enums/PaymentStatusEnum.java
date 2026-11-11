package bo.cdr.app.common.enums;

import bo.cdr.app.exception.InvalidEnumValueException;
import lombok.Getter;

import java.util.Locale;

/** Estado de un pago. */
@Getter
public enum PaymentStatusEnum {
    PAID("Pagado"),
    REFUNDED("Reembolsado");

    /** Etiqueta en español para la UI. */
    private final String label;

    PaymentStatusEnum(String label) {
        this.label = label;
    }

    /** Acepta el name() del enum (lo que viaja en la API). */
    public static PaymentStatusEnum fromString(String text) {
        if (text != null) {
            String normalized = text.trim().toUpperCase(Locale.ROOT);
            for (PaymentStatusEnum value : values()) {
                if (value.name().equals(normalized)) {
                    return value;
                }
            }
        }
        throw new InvalidEnumValueException("Estado del pago", text);
    }
}
