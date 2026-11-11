package bo.cdr.app.appsecurity.presentation.request.dto;

import bo.cdr.app.config.StringOnlyDeserializer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.annotation.JsonDeserialize;

/** Restablecimiento de contraseña por un ADMIN (no hay recuperación por correo en la v1). */
@Data
@NoArgsConstructor
public class UserPasswordDto {

    @NotBlank(message = "{user.password.not-blank}")
    @JsonDeserialize(using = StringOnlyDeserializer.class)
    @Size(min = 8, max = 72, message = "{user.password.size}")
    private String password;
}
