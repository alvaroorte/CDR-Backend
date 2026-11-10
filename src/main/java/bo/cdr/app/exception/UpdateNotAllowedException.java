package bo.cdr.app.exception;

public class UpdateNotAllowedException extends RuntimeException{
    public UpdateNotAllowedException(String message) {
        super(message);
    }
}
