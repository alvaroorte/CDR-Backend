package bo.cdr.app.common.enums;

import bo.cdr.app.exception.InvalidEnumValueException;
import lombok.Getter;

import java.util.Locale;

/** Modalidad del pedido. */
@Getter
public enum ServiceTypeEnum {
    DINE_IN("En el local"),
    TAKEAWAY("Para llevar");

    /** Etiqueta en español para la UI. */
    private final String label;

    ServiceTypeEnum(String label) {
        this.label = label;
    }

    /** Acepta el name() del enum (lo que viaja en la API). */
    public static ServiceTypeEnum fromString(String text) {
        if (text != null) {
            String normalized = text.trim().toUpperCase(Locale.ROOT);
            for (ServiceTypeEnum value : values()) {
                if (value.name().equals(normalized)) {
                    return value;
                }
            }
        }
        throw new InvalidEnumValueException("Modalidad", text);
    }
}
