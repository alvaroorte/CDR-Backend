package bo.cdr.app.apporder.presentation.request.dto;

import bo.cdr.app.common.custom.validator.ValidEnum;
import bo.cdr.app.common.enums.OrderStatusEnum;
import bo.cdr.app.config.StringOnlyDeserializer;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.annotation.JsonDeserialize;

/** Cambio de estado. {@code expectedVersion} = versión que el cliente vio (409 si otro la cambió). */
@Data
@NoArgsConstructor
public class OrderStatusDto {

    @ValidEnum(enumClass = OrderStatusEnum.class, message = "{order.status.invalid}")
    @JsonDeserialize(using = StringOnlyDeserializer.class)
    private String toStatus;

    @NotNull(message = "{order.expectedVersion.not-null}")
    @PositiveOrZero(message = "{order.expectedVersion.not-null}")
    private Long expectedVersion;

    @JsonDeserialize(using = StringOnlyDeserializer.class)
    @Size(max = 255, message = "{order.note.size}")
    private String note;
}
