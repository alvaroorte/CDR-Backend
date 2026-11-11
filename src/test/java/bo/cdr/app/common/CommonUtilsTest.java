package bo.cdr.app.common;

import bo.cdr.app.common.custom.validator.ImageBase64Validator;
import bo.cdr.app.common.custom.validator.IsImageBase64;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.math.BigDecimal;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class CommonUtilsTest {

    private static final String PNG_1X1 =
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==";

    private static ImageBase64Validator validator(long maxBytes) {
        ImageBase64Validator v = new ImageBase64Validator();
        v.initialize(new IsImageBase64() {
            public String message() { return ""; }
            public long maxBytes() { return maxBytes; }
            public Class<?>[] groups() { return new Class[0]; }
            @SuppressWarnings("unchecked")
            public Class<? extends jakarta.validation.Payload>[] payload() { return new Class[0]; }
            public Class<? extends Annotation> annotationType() { return IsImageBase64.class; }
        });
        return v;
    }

    @Test
    void moneyUtil_roundsHalfUpToTwoDecimals() {
        assertThat(MoneyUtil.round(new BigDecimal("2.345"))).isEqualByComparingTo("2.35");
        assertThat(MoneyUtil.round(new BigDecimal("2.344"))).isEqualByComparingTo("2.34");
        assertThat(MoneyUtil.round(new BigDecimal("15")).scale()).isEqualTo(2);
    }

    @Test
    void moneyUtil_percentOf() {
        assertThat(MoneyUtil.percentOf(new BigDecimal("45.00"), new BigDecimal("10"))).isEqualByComparingTo("4.50");
        assertThat(MoneyUtil.percentOf(new BigDecimal("33.33"), new BigDecimal("15"))).isEqualByComparingTo("5.00");
    }

    @Test
    void imageValidator_acceptsPngAndDataUrl_andEmpty() {
        ImageBase64Validator v = validator(1_048_576);
        assertThat(v.isValid(PNG_1X1, null)).isTrue();
        assertThat(v.isValid("data:image/png;base64," + PNG_1X1, null)).isTrue();
        assertThat(v.isValid(null, null)).isTrue();
        assertThat(v.isValid("", null)).isTrue();
    }

    @Test
    void imageValidator_rejectsNonImageAndInvalidBase64() {
        ImageBase64Validator v = validator(1_048_576);
        assertThat(v.isValid(Base64.getEncoder().encodeToString("hola mundo, no soy imagen".getBytes()), null)).isFalse();
        assertThat(v.isValid("%%%", null)).isFalse();
    }

    @Test
    void imageValidator_rejectsImagesAboveMaxSize() {
        assertThat(validator(10).isValid(PNG_1X1, null)).isFalse();
    }
}
