package bo.cdr.app.common.enums;

import bo.cdr.app.exception.InvalidEnumValueException;
import lombok.Getter;

import java.util.Locale;

/** Reportes exportables a Excel (Etapa 6). El label se usa como nombre del archivo. */
@Getter
public enum ReportTypeEnum {
    SALES("Ventas"),
    PRODUCTS("Productos y categorías"),
    PAYMENTS("Medios de pago y cajas"),
    OPERATIONS("Usuarios, descuentos y estados");

    /** Etiqueta en español para la UI. */
    private final String label;

    ReportTypeEnum(String label) {
        this.label = label;
    }

    /** Acepta el name() del enum (lo que viaja en la API). */
    public static ReportTypeEnum fromString(String text) {
        if (text != null) {
            String normalized = text.trim().toUpperCase(Locale.ROOT);
            for (ReportTypeEnum value : values()) {
                if (value.name().equals(normalized)) {
                    return value;
                }
            }
        }
        throw new InvalidEnumValueException("Reporte", text);
    }
}
