package bo.cdr.app.exception;

/**
 * Se superó el máximo de intentos fallidos de login para una cuenta o una IP.
 * Se mapea a 429 (TOO_MANY_REQUESTS) en {@link ErrorHandler}.
 */
public class TooManyLoginAttemptsException extends RuntimeException {

    public TooManyLoginAttemptsException(String message) {
        super(message);
    }
}
