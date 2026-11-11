package bo.cdr.app.common.custom.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.lang.reflect.Method;

public class EnumValidator implements ConstraintValidator<ValidEnum, String> {
    private Class<? extends Enum<?>> enumClass;
    private String methodName;

    @Override
    public void initialize(ValidEnum constraintAnnotation) {
        this.enumClass = constraintAnnotation.enumClass();
        this.methodName = constraintAnnotation.method();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return false;
        }

        try {
            Method fromStringMethod = enumClass.getMethod("fromString", String.class);

            fromStringMethod.invoke(null, value);
            return true;

        } catch (NoSuchMethodException e) {
            throw new RuntimeException("El enum debe tener un método fromString(String)");
        } catch (Exception e) {
            return false;
        }
    }
}