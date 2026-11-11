package bo.cdr.app.common.enums;

import bo.cdr.app.exception.InvalidEnumValueException;
import lombok.Getter;

import java.util.Locale;

/** Tipo del descuento general del pedido. */
@Getter
public enum DiscountTypeEnum {
    PERCENTAGE("Porcentaje"),
    FIXED_AMOUNT("Monto fijo");

    /** Etiqueta en español para la UI. */
    private final String label;

    DiscountTypeEnum(String label) {
        this.label = label;
    }

    /** Acepta el name() del enum (lo que viaja en la API). */
    public static DiscountTypeEnum fromString(String text) {
        if (text != null) {
            String normalized = text.trim().toUpperCase(Locale.ROOT);
            for (DiscountTypeEnum value : values()) {
                if (value.name().equals(normalized)) {
                    return value;
                }
            }
        }
        throw new InvalidEnumValueException("Tipo de descuento", text);
    }
}
