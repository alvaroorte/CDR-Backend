package bo.cdr.app.exception;


import bo.cdr.app.exception.response.ErrorResponse;
import bo.cdr.app.exception.response.FieldErrorModel;
import bo.cdr.app.exception.response.ValidationErrorException;
import bo.cdr.app.exception.response.ValidationErrorResponse;
import bo.cdr.app.util.Messages;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

// Acotado a los controladores propios (bo.cdr.app), igual que la referencia: evita interceptar
// excepciones de controladores internos de Spring como BasicErrorController,
// cuyo manejo del error de transportes SockJS (/ws-tickets/**, respuesta ya
// comprometida con Content-Type application/javascript) generaba cascadas de
// ERROR/stacktrace inescribibles en los logs.
@ControllerAdvice(basePackages = "bo.cdr.app")
public class ErrorHandler {

  private static final Logger log = LoggerFactory.getLogger(ErrorHandler.class);


  @ExceptionHandler({HasAssociatedEntityException.class, InvalidValueException.class})
  public ResponseEntity<ErrorResponse> handlerBadRequestException(HttpServletRequest req, Exception ex) {
    HttpStatus status = HttpStatus.BAD_REQUEST;
    ErrorResponse response = new ErrorResponse(status.value(), status.name(), ex.getMessage(), req.getRequestURI());
    return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(response);
  }

  @ExceptionHandler(TooManyLoginAttemptsException.class)
  public ResponseEntity<ErrorResponse> handlerTooManyLoginAttempts(HttpServletRequest req, Exception ex) {
    HttpStatus status = HttpStatus.TOO_MANY_REQUESTS;
    ErrorResponse response = new ErrorResponse(status.value(), status.name(), ex.getMessage(), req.getRequestURI());
    return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(response);
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ErrorResponse> handleMethodNotAllowed(
          HttpServletRequest request,
          HttpRequestMethodNotSupportedException ex) {

    HttpStatus status = HttpStatus.METHOD_NOT_ALLOWED;

    ErrorResponse response = new ErrorResponse(
            status.value(),
            status.name(),
            "Método HTTP no soportado para este endpoint",
            request.getRequestURI()
    );

    return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(response);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ValidationErrorResponse> handleValidException(
          HttpServletRequest req, MethodArgumentNotValidException ex) {

    List<FieldErrorModel> errors = ex.getBindingResult().getAllErrors().stream().map(fieldError -> {
              FieldErrorModel fieldErrorModel = new FieldErrorModel();
              fieldErrorModel.setCode(fieldError.getCode());
              fieldErrorModel.setMessage(fieldError.getDefaultMessage());
              fieldErrorModel.setField(((FieldError) fieldError).getField());
              return fieldErrorModel;
            }).sorted(Comparator.comparing(FieldErrorModel::getField)
                    .thenComparing(FieldErrorModel::getCode)
                    .thenComparing(FieldErrorModel::getMessage))
            .toList();

    HttpStatus status = HttpStatus.BAD_REQUEST;
    ValidationErrorResponse response = new ValidationErrorResponse();
    response.setCode(status.value());
    response.setStatus(status.name());
    response.setPath(req.getRequestURI());
    response.setErrors(errors);

    return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(response);
  }

  @ExceptionHandler({ValidationErrorException.class, UpdateNotAllowedException.class, NotAllowedException.class, InvalidEnumValueException.class, MissingParameterException.class, DisabledEntityException.class})
  public ResponseEntity<ErrorResponse> handlerValidationErrorException(HttpServletRequest req,
      Exception ex) {
    HttpStatus status = HttpStatus.BAD_REQUEST;
    ErrorResponse response = new ErrorResponse(status.value(), status.name(), ex.getMessage(),
        req.getRequestURI());
    return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(response);
  }

  // NUEVO vs referencia (allí caía en el 500 genérico): cuerpo JSON mal formado o con tipos
  // inválidos (p. ej. StringOnlyDeserializer rechaza un número) → 400.
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleNotReadable(HttpServletRequest req, HttpMessageNotReadableException ex) {
    HttpStatus status = HttpStatus.BAD_REQUEST;
    ErrorResponse response = new ErrorResponse(status.value(), status.name(),
            "El cuerpo de la solicitud es inválido o tiene tipos de datos incorrectos.", req.getRequestURI());
    return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(response);
  }

  @ExceptionHandler(EntityNotFoundException.class)
  public ResponseEntity<ErrorResponse> handlerEntityNotFoundException(HttpServletRequest req,
                                                                      Exception ex) {
    HttpStatus status = HttpStatus.NOT_FOUND;
    ErrorResponse response = new ErrorResponse(status.value(), status.name(), ex.getMessage(),
            req.getRequestURI());
    return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(response);
  }

  @ExceptionHandler({DuplicateException.class, ConflictException.class})
  public  ResponseEntity<ErrorResponse> handlerDuplicateException(HttpServletRequest req, Exception ex){
    HttpStatus status = HttpStatus.CONFLICT;
    ErrorResponse response = new ErrorResponse(status.value(), status.name(), ex.getMessage(), req.getRequestURI());
    return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(response);
  }

  @ExceptionHandler(AlreadyDeletedException.class)
  public ResponseEntity<ErrorResponse> handlerAlreadyDeletedException(HttpServletRequest req, Exception ex){
    HttpStatus status = HttpStatus.NOT_FOUND;
    ErrorResponse response = new ErrorResponse(status.value(), status.name(), ex.getMessage(), req.getRequestURI());
    return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(response);
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ErrorResponse> handleMissingServletRequestParameterException(
          MissingServletRequestParameterException ex, HttpServletRequest req) {
    HttpStatus status = HttpStatus.BAD_REQUEST;
    String errorMessage = String.format(
            Messages.getProperty("exception.missingServletRequestParameterException.message"),
            ex.getParameterName());
    ErrorResponse response = new ErrorResponse(status.value(), status.getReasonPhrase(),
            errorMessage, req.getRequestURI());
    return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(response);
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(
          MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
    HttpStatus status = HttpStatus.BAD_REQUEST;
    String errorMessage = String.format(
            Messages.getProperty("exception.methodArgumentTypeMismatchException.message"), ex.getName(),
            ex.getRequiredType().getSimpleName());
    ErrorResponse response = new ErrorResponse(status.value(), status.getReasonPhrase(),
            errorMessage, req.getRequestURI());
    return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(response);
  }

  @ExceptionHandler(UnauthorizedException.class)
  public ResponseEntity<ErrorResponse> handleUnauthorizedException(HttpServletRequest req, UnauthorizedException ex) {
    HttpStatus status = HttpStatus.UNAUTHORIZED;
    ErrorResponse response = new ErrorResponse(status.value(), status.name(), ex.getMessage(), req.getRequestURI());
    return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(response);
  }

  // Usuario desactivado (DaoAuthenticationProvider) → 401 con mensaje claro.
  @ExceptionHandler(DisabledException.class)
  public ResponseEntity<ErrorResponse> handleDisabledException(HttpServletRequest req, DisabledException ex) {
    HttpStatus status = HttpStatus.UNAUTHORIZED;
    ErrorResponse response = new ErrorResponse(status.value(), status.name(), "Usuario deshabilitado", req.getRequestURI());
    return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(response);
  }

  // NUEVO vs referencia: conflictos de concurrencia (@Version) y violaciones de unicidad → 409.
  @ExceptionHandler({ObjectOptimisticLockingFailureException.class, DataIntegrityViolationException.class})
  public ResponseEntity<ErrorResponse> handleConflict(HttpServletRequest req, Exception ex) {
    HttpStatus status = HttpStatus.CONFLICT;
    String message = ex instanceof ObjectOptimisticLockingFailureException
            ? "El registro fue modificado por otro usuario. Recargue e intente de nuevo."
            : "La operación viola una restricción de integridad de datos.";
    log.warn("Conflicto en {} {}: {}", req.getMethod(), req.getRequestURI(), ex.getMessage());
    ErrorResponse response = new ErrorResponse(status.value(), status.name(), message, req.getRequestURI());
    return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(response);
  }

  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<ErrorResponse> handleBadCredentialsException(HttpServletRequest req, BadCredentialsException ex) {
    HttpStatus status = HttpStatus.UNAUTHORIZED;
    ErrorResponse response = new ErrorResponse(status.value(), status.name(), "Usuario o contraseña incorrectos", req.getRequestURI());
    return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(response);
  }

  @ExceptionHandler(NoHandlerFoundException.class)
  public ResponseEntity<ErrorResponse> handleNotFound(NoHandlerFoundException ex, HttpServletRequest req) {

      HttpStatus status = HttpStatus.NOT_FOUND;
      ErrorResponse response = new ErrorResponse(status.value(), status.name(), ex.getMessage(), ex.getRequestURL());

      return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(response);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handlerGenericException(HttpServletRequest req, Exception ex) {
    log.error("Unhandled exception on {} {}: {}", req.getMethod(), req.getRequestURI(), ex.getMessage(), ex);
    HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
    ErrorResponse response = new ErrorResponse(status.value(), status.name(),
            "Error interno del servidor", req.getRequestURI());
    return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(response);
  }
}
