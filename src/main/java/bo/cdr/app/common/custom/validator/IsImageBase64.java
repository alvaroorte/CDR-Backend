package bo.cdr.app.common.custom.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Imagen en base64 (patrón de la referencia). Nulo o vacío = sin imagen (válido).
 * NUEVO vs referencia: límite de tamaño ({@link #maxBytes()}, por defecto 1 MB, decisión aprobada)
 * y se acepta el prefijo data URL ("data:image/png;base64,...") que envía el navegador.
 */
@Documented
@Constraint(validatedBy = ImageBase64Validator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface IsImageBase64 {
    String message() default "Formato de imagen Base64 no válido o la imagen supera el tamaño máximo";
    long maxBytes() default 1_048_576L;
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
