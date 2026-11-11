package bo.cdr.app.common.custom.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = EnumValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidEnum {
    Class<? extends Enum<?>> enumClass();

    String method() default "name"; // Método por defecto es name()

    String message() default "Invalid value. Must be one of the predefined values.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
