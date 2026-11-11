package bo.cdr.app.appcatalog.presentation.request.dto;

import bo.cdr.app.config.StringOnlyDeserializer;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class AddonDto {

    @NotBlank(message = "{addon.name.not-blank}")
    @JsonDeserialize(using = StringOnlyDeserializer.class)
    @Size(min = 2, max = 100, message = "{addon.name.size}")
    private String name;

    @NotNull(message = "{common.price.not-null}")
    @DecimalMin(value = "0.00", message = "{common.price.min}")
    @Digits(integer = 10, fraction = 2, message = "{common.price.digits}")
    private BigDecimal price;

    @NotNull(message = "{common.active.not-null}")
    private Boolean active;
}
