package bo.cdr.app.apporder.presentation.request.dto;

import bo.cdr.app.config.StringOnlyDeserializer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.annotation.JsonDeserialize;

/** Cancelación con motivo obligatorio (skill cdr-order-management §9). */
@Data
@NoArgsConstructor
public class OrderCancelDto {

    @NotBlank(message = "{order.cancelReason.not-blank}")
    @JsonDeserialize(using = StringOnlyDeserializer.class)
    @Size(max = 255, message = "{order.cancelReason.size}")
    private String reason;

    @NotNull(message = "{order.expectedVersion.not-null}")
    @PositiveOrZero(message = "{order.expectedVersion.not-null}")
    private Long expectedVersion;
}
