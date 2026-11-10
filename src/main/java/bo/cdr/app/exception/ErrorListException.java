package bo.cdr.app.exception;

import java.util.List;
import lombok.Getter;

@Getter
public class ErrorListException extends RuntimeException {
    private final List<?> errorList;

    public ErrorListException(List<?> errorList) {
        this.errorList = errorList;
    }
}

