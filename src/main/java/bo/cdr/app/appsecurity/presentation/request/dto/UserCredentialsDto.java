package bo.cdr.app.appsecurity.presentation.request.dto;

import bo.cdr.app.config.StringOnlyDeserializer;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.annotation.JsonDeserialize;

/**
 * Credenciales de login. Login por username (decisión aprobada). El password llega en base64
 * (patrón de la referencia; no es cifrado, la protección real es HTTPS).
 */
@Data
@NoArgsConstructor
public class UserCredentialsDto {

    @NotBlank(message = "{auth.username.not-blank}")
    @JsonDeserialize(using = StringOnlyDeserializer.class)
    private String username;

    @NotBlank(message = "{auth.password.not-blank}")
    @JsonDeserialize(using = StringOnlyDeserializer.class)
    private String password;
}
