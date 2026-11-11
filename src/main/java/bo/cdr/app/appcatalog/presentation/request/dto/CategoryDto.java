package bo.cdr.app.appcatalog.presentation.request.dto;

import bo.cdr.app.common.custom.validator.IsImageBase64;
import bo.cdr.app.config.IntegerOnlyDeserializer;
import bo.cdr.app.config.StringOnlyDeserializer;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.annotation.JsonDeserialize;

@Data
@NoArgsConstructor
public class CategoryDto {

    @NotBlank(message = "{category.name.not-blank}")
    @JsonDeserialize(using = StringOnlyDeserializer.class)
    @Size(min = 2, max = 100, message = "{category.name.size}")
    private String name;

    @JsonDeserialize(using = StringOnlyDeserializer.class)
    @Size(max = 500, message = "{common.description.size}")
    private String description;

    @IsImageBase64(message = "{common.image.invalid}")
    private String imageBase64;

    @NotNull(message = "{category.ranking.not-null}")
    @JsonDeserialize(using = IntegerOnlyDeserializer.class)
    @Min(value = 0, message = "{category.ranking.min}")
    @Max(value = 999, message = "{category.ranking.max}")
    private Integer ranking;

    @NotNull(message = "{common.active.not-null}")
    private Boolean active;
}
