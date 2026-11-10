package bo.cdr.app.exception;

import bo.cdr.app.util.Messages;

public class InvalidValueException extends RuntimeException {

    public InvalidValueException(String columnName, String invalidValue) {
        super(String.format(Messages.getProperty("exception.invalidValue.message"), invalidValue, columnName));
    }
}
