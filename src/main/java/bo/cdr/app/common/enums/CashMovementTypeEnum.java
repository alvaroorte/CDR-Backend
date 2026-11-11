package bo.cdr.app.common.enums;

import bo.cdr.app.exception.InvalidEnumValueException;
import lombok.Getter;

import java.util.Locale;

/** Tipos de movimiento de caja. OPENING/INCOME/EXPENSE son efectivo. */
@Getter
public enum CashMovementTypeEnum {
    OPENING("Apertura"),
    SALE("Venta"),
    REFUND("Devolución"),
    INCOME("Ingreso"),
    EXPENSE("Egreso");

    /** Etiqueta en español para la UI. */
    private final String label;

    CashMovementTypeEnum(String label) {
        this.label = label;
    }

    /** Acepta el name() del enum (lo que viaja en la API). */
    public static CashMovementTypeEnum fromString(String text) {
        if (text != null) {
            String normalized = text.trim().toUpperCase(Locale.ROOT);
            for (CashMovementTypeEnum value : values()) {
                if (value.name().equals(normalized)) {
                    return value;
                }
            }
        }
        throw new InvalidEnumValueException("Tipo de movimiento", text);
    }
}
