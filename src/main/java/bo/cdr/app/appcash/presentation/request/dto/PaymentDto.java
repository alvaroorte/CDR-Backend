package bo.cdr.app.appcash.presentation.request.dto;

import bo.cdr.app.config.IntegerOnlyDeserializer;
import bo.cdr.app.config.StringOnlyDeserializer;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.math.BigDecimal;

/**
 * Pago de un pedido: importe APLICADO al pedido. El importe recibido y el vuelto no viajan
 * (solo se muestran en el POS).
 */
@Data
@NoArgsConstructor
public class PaymentDto {

    @NotNull(message = "{payment.paymentMethodId.not-null}")
    @Positive(message = "{payment.paymentMethodId.not-null}")
    @JsonDeserialize(using = IntegerOnlyDeserializer.class)
    private Integer paymentMethodId;

    @NotNull(message = "{common.amount.not-null}")
    @DecimalMin(value = "0.01", message = "{common.amount.positive}")
    @Digits(integer = 10, fraction = 2, message = "{common.amount.digits}")
    private BigDecimal amount;

    @JsonDeserialize(using = StringOnlyDeserializer.class)
    @Size(max = 100, message = "{payment.reference.size}")
    private String reference;
}
