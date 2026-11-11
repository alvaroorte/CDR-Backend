package bo.cdr.app.appcatalog.presentation.request.dto;

import bo.cdr.app.config.IntegerOnlyDeserializer;
import bo.cdr.app.config.StringOnlyDeserializer;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.annotation.JsonDeserialize;

@Data
@NoArgsConstructor
public class TableMarkerDto {

    @NotNull(message = "{tableMarker.number.not-null}")
    @JsonDeserialize(using = IntegerOnlyDeserializer.class)
    @Min(value = 1, message = "{tableMarker.number.min}")
    @Max(value = 999, message = "{tableMarker.number.max}")
    private Integer number;

    @JsonDeserialize(using = StringOnlyDeserializer.class)
    @Size(max = 255, message = "{tableMarker.description.size}")
    private String description;

    @NotNull(message = "{common.active.not-null}")
    private Boolean active;
}
