package bo.cdr.app.appcatalog.presentation.request.dto;

import bo.cdr.app.config.StringOnlyDeserializer;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.annotation.JsonDeserialize;

/** El código solo se usa al crear; al editar se ignora (es el identificador estable del medio). */
@Data
@NoArgsConstructor
public class PaymentMethodDto {

    @NotBlank(message = "{paymentMethod.code.not-blank}")
    @JsonDeserialize(using = StringOnlyDeserializer.class)
    @Pattern(regexp = "^[A-Za-z0-9_]{2,30}$", message = "{paymentMethod.code.pattern}")
    private String code;

    @NotBlank(message = "{paymentMethod.name.not-blank}")
    @JsonDeserialize(using = StringOnlyDeserializer.class)
    @Size(min = 2, max = 100, message = "{paymentMethod.name.size}")
    private String name;

    @NotNull(message = "{paymentMethod.requiresReference.not-null}")
    private Boolean requiresReference;

    @NotNull(message = "{paymentMethod.givesChange.not-null}")
    private Boolean givesChange;

    @NotNull(message = "{common.active.not-null}")
    private Boolean active;
}
