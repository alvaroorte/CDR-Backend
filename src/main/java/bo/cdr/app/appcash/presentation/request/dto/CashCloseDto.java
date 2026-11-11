package bo.cdr.app.appcash.presentation.request.dto;

import bo.cdr.app.config.StringOnlyDeserializer;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.util.ArrayList;
import java.util.List;

/** Cierre con arqueo: un importe declarado por cada medio del preview. Nota obligatoria si un ADMIN cierra la caja de otro. */
@Data
@NoArgsConstructor
public class CashCloseDto {

    @NotNull(message = "{cash.expectedVersion.not-null}")
    @PositiveOrZero(message = "{cash.expectedVersion.not-null}")
    private Long expectedVersion;

    @NotEmpty(message = "{cash.declared.not-empty}")
    @Valid
    private List<CashDeclaredDto> declared = new ArrayList<>();

    @JsonDeserialize(using = StringOnlyDeserializer.class)
    @Size(max = 500, message = "{cash.note.size}")
    private String note;
}
