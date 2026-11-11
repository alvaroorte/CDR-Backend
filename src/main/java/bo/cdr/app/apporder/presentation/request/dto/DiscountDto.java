package bo.cdr.app.apporder.presentation.request.dto;

import bo.cdr.app.common.custom.validator.ValidEnum;
import bo.cdr.app.common.enums.DiscountTypeEnum;
import bo.cdr.app.config.StringOnlyDeserializer;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.math.BigDecimal;

/** Descuento general del pedido. Límites por rol y motivo obligatorio: OrderPricingService. */
@Data
@NoArgsConstructor
public class DiscountDto {

    @ValidEnum(enumClass = DiscountTypeEnum.class, message = "{order.discount.type.invalid}")
    @JsonDeserialize(using = StringOnlyDeserializer.class)
    private String type;

    @NotNull(message = "{order.discount.value.not-null}")
    @DecimalMin(value = "0.01", message = "{order.discount.value.positive}")
    @Digits(integer = 10, fraction = 2, message = "{common.amount.digits}")
    private BigDecimal value;

    @JsonDeserialize(using = StringOnlyDeserializer.class)
    @Size(max = 255, message = "{order.discount.reason.size}")
    private String reason;
}
