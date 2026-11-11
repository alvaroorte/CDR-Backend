package bo.cdr.app.common.enums;

import bo.cdr.app.exception.InvalidEnumValueException;
import lombok.Getter;

import java.util.Locale;

/** Estado de la sesión de caja. */
@Getter
public enum CashSessionStatusEnum {
    OPEN("Abierta"),
    CLOSED("Cerrada");

    /** Etiqueta en español para la UI. */
    private final String label;

    CashSessionStatusEnum(String label) {
        this.label = label;
    }

    /** Acepta el name() del enum (lo que viaja en la API). */
    public static CashSessionStatusEnum fromString(String text) {
        if (text != null) {
            String normalized = text.trim().toUpperCase(Locale.ROOT);
            for (CashSessionStatusEnum value : values()) {
                if (value.name().equals(normalized)) {
                    return value;
                }
            }
        }
        throw new InvalidEnumValueException("Estado de caja", text);
    }
}
