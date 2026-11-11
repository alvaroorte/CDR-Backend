package bo.cdr.app.exception;

/**
 * Conflicto de negocio que el usuario puede resolver recargando o eligiendo otra opción
 * (número de mesa en uso, caja ya abierta...) → 409 con mensaje específico.
 */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
