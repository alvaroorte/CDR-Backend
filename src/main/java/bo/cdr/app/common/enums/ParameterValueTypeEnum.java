package bo.cdr.app.common.enums;

import bo.cdr.app.exception.InvalidEnumValueException;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/** Tipo de valor de un parámetro: determina cómo se valida al editarlo. */
@Getter
public enum ParameterValueTypeEnum {
    PERCENTAGE("PORCENTAJE"),
    NUMBER("NÚMERO"),
    TEXT("TEXTO"),
    BOOLEAN("SI/NO");

    private final String name;

    ParameterValueTypeEnum(String name) {
        this.name = name;
    }

    private static final Map<String, ParameterValueTypeEnum> lookup = new HashMap<>();

    static {
        for (ParameterValueTypeEnum type : values()) {
            lookup.put(type.getName(), type);
        }
    }

    public static ParameterValueTypeEnum fromString(String text) {
        if (text == null || !lookup.containsKey(text.toUpperCase())) {
            throw new InvalidEnumValueException("Tipo de valor", text);
        }
        return lookup.get(text.toUpperCase());
    }
}
