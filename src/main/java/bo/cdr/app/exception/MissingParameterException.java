package bo.cdr.app.exception;

import bo.cdr.app.util.Messages;

public class MissingParameterException extends RuntimeException {
    public MissingParameterException(String parameterName) {
        super(String.format(Messages.getProperty("exception.missingParameter.message"), parameterName));
    }
}