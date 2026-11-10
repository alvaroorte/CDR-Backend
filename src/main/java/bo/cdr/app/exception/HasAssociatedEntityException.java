package bo.cdr.app.exception;

import bo.cdr.app.util.Messages;

public class HasAssociatedEntityException extends RuntimeException {

    public HasAssociatedEntityException(String entity, String associatedEntity) {
        super(String.format(Messages.getProperty("exception.hasAssociatedEntity.message"), entity, associatedEntity));
    }
}
