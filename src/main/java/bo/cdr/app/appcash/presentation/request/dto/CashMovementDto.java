package bo.cdr.app.appcash.presentation.request.dto;

import bo.cdr.app.common.custom.validator.ValidEnum;
import bo.cdr.app.common.enums.CashMovementTypeEnum;
import bo.cdr.app.config.StringOnlyDeserializer;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.math.BigDecimal;

/** Movimiento manual de caja. Solo INCOME o EXPENSE (lo valida el servicio). */
@Data
@NoArgsConstructor
public class CashMovementDto {

    @ValidEnum(enumClass = CashMovementTypeEnum.class, message = "{cash.movement.type.invalid}")
    @JsonDeserialize(using = StringOnlyDeserializer.class)
    private String type;

    @NotNull(message = "{common.amount.not-null}")
    @DecimalMin(value = "0.01", message = "{common.amount.positive}")
    @Digits(integer = 10, fraction = 2, message = "{common.amount.digits}")
    private BigDecimal amount;

    @NotBlank(message = "{cash.movement.description.not-blank}")
    @JsonDeserialize(using = StringOnlyDeserializer.class)
    @Size(max = 255, message = "{cash.movement.description.size}")
    private String description;
}
