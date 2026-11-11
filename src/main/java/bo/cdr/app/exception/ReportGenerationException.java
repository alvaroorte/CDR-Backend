package bo.cdr.app.exception;

/** Fallo técnico al generar un archivo de reporte (se responde 500 con el mensaje genérico). */
public class ReportGenerationException extends RuntimeException {
    public ReportGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
