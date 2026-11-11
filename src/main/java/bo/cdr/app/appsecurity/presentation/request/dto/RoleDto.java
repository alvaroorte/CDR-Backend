package bo.cdr.app.appsecurity.presentation.request.dto;

import bo.cdr.app.config.StringOnlyDeserializer;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.annotation.JsonDeserialize;

/** El código solo se usa al crear (los roles del sistema nunca cambian de código). */
@Data
@NoArgsConstructor
public class RoleDto {

    @NotBlank(message = "{role.name.not-blank}")
    @JsonDeserialize(using = StringOnlyDeserializer.class)
    @Size(min = 3, max = 100, message = "{role.name.size}")
    private String name;

    @NotBlank(message = "{role.code.not-blank}")
    @JsonDeserialize(using = StringOnlyDeserializer.class)
    @Pattern(regexp = "^[A-Za-z_]{3,15}$", message = "{role.code.pattern}")
    private String code;

    @JsonDeserialize(using = StringOnlyDeserializer.class)
    @Size(max = 255, message = "{role.description.size}")
    private String description;

    @NotNull(message = "{common.active.not-null}")
    private Boolean active;
}
