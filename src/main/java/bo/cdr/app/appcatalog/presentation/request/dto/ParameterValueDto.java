package bo.cdr.app.appcatalog.presentation.request.dto;

import bo.cdr.app.config.StringOnlyDeserializer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.annotation.JsonDeserialize;

/** Solo se edita el valor; la validación según el tipo la hace el servicio. */
@Data
@NoArgsConstructor
public class ParameterValueDto {

    @NotBlank(message = "{parameter.value.not-blank}")
    @JsonDeserialize(using = StringOnlyDeserializer.class)
    @Size(max = 255, message = "{parameter.value.size}")
    private String value;
}
