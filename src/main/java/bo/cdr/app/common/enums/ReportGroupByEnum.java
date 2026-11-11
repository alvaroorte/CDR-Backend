package bo.cdr.app.common.enums;

import bo.cdr.app.exception.InvalidEnumValueException;
import lombok.Getter;

import java.util.Locale;

/** Agrupación del reporte de ventas por período. {@code unit} es el argumento de date_trunc (lista cerrada). */
@Getter
public enum ReportGroupByEnum {
    DAY("day", "Día"),
    WEEK("week", "Semana"),
    MONTH("month", "Mes");

    /** Unidad de PostgreSQL date_trunc; nunca viene del cliente. */
    private final String unit;

    /** Etiqueta en español para la UI. */
    private final String label;

    ReportGroupByEnum(String unit, String label) {
        this.unit = unit;
        this.label = label;
    }

    /** Acepta el name() del enum; null o vacío → DAY. */
    public static ReportGroupByEnum fromString(String text) {
        if (text == null || text.isBlank()) {
            return DAY;
        }
        String normalized = text.trim().toUpperCase(Locale.ROOT);
        for (ReportGroupByEnum value : values()) {
            if (value.name().equals(normalized)) {
                return value;
            }
        }
        throw new InvalidEnumValueException("Agrupación", text);
    }
}
