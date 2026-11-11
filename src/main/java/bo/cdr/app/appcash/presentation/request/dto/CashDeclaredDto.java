package bo.cdr.app.appcash.presentation.request.dto;

import bo.cdr.app.config.IntegerOnlyDeserializer;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.math.BigDecimal;

/** Importe contado / declarado de un medio de pago en el arqueo. */
@Data
@NoArgsConstructor
public class CashDeclaredDto {

    @NotNull(message = "{payment.paymentMethodId.not-null}")
    @JsonDeserialize(using = IntegerOnlyDeserializer.class)
    private Integer paymentMethodId;

    @NotNull(message = "{cash.declared.not-null}")
    @DecimalMin(value = "0.00", message = "{cash.declared.min}")
    @Digits(integer = 10, fraction = 2, message = "{common.amount.digits}")
    private BigDecimal amount;
}
