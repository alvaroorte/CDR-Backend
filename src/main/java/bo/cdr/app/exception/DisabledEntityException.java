package bo.cdr.app.exception;

import bo.cdr.app.util.Messages;

public class DisabledEntityException extends RuntimeException {
    public DisabledEntityException(String entity, Integer id) {
        super(String.format(Messages.getProperty("exception.disabledEntityException.message"), entity, id));
    }
}
