package bo.cdr.app.common.enums;

import bo.cdr.app.exception.InvalidEnumValueException;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/** Carpetas de parámetros (patrón ParameterFolderEnum de la referencia). */
@Getter
public enum ParameterFolderEnum {
    SYSTEM("SISTEMA"),
    DISCOUNTS("DESCUENTOS"),
    KITCHEN("COCINA");

    private final String name;

    ParameterFolderEnum(String name) {
        this.name = name;
    }

    private static final Map<String, ParameterFolderEnum> lookup = new HashMap<>();

    static {
        for (ParameterFolderEnum folder : values()) {
            lookup.put(folder.getName(), folder);
        }
    }

    public static ParameterFolderEnum fromString(String text) {
        if (text == null || !lookup.containsKey(text.toUpperCase())) {
            throw new InvalidEnumValueException("Carpeta de parámetro", text);
        }
        return lookup.get(text.toUpperCase());
    }
}
