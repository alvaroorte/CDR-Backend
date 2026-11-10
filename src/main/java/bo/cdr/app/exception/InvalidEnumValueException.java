package bo.cdr.app.exception;

public class InvalidEnumValueException extends RuntimeException {
    public InvalidEnumValueException(String enumType, String invalidValue) {
        super(String.format("Valor inválido '%s' para %s", invalidValue, enumType));
    }
}
