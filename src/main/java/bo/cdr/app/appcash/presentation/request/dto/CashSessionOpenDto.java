package bo.cdr.app.appcash.presentation.request.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class CashSessionOpenDto {

    @NotNull(message = "{cash.openingAmount.not-null}")
    @DecimalMin(value = "0.00", message = "{cash.openingAmount.min}")
    @Digits(integer = 10, fraction = 2, message = "{common.amount.digits}")
    private BigDecimal openingAmount;
}
