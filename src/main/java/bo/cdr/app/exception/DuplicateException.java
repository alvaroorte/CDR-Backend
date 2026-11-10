package bo.cdr.app.exception;

import bo.cdr.app.util.Messages;

public class DuplicateException extends RuntimeException {

    public DuplicateException(String message) {
        super(message);
    }

    public DuplicateException(String entityName, String columnName, String value) {
        super(String.format(Messages.getProperty("exception.duplicate.message"), entityName, columnName, value));
    }

    public DuplicateException(String entityName, int id) {
        super(String.format("%s con el ID %s ya existe", entityName, id));
    }
}
