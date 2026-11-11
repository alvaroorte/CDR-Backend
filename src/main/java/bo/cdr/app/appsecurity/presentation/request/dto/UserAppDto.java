package bo.cdr.app.appsecurity.presentation.request.dto;

import bo.cdr.app.config.IntegerListDeserializer;
import bo.cdr.app.config.StringOnlyDeserializer;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.util.ArrayList;
import java.util.List;

/**
 * Alta/edición de usuario. {@code password} es obligatorio al crear y se ignora al editar
 * (para cambiarlo: PATCH /users/{id}/password). Su obligatoriedad la valida el servicio.
 */
@Data
@NoArgsConstructor
public class UserAppDto {

    @NotBlank(message = "{user.username.not-blank}")
    @JsonDeserialize(using = StringOnlyDeserializer.class)
    @Pattern(regexp = "^[A-Za-z0-9._-]{3,50}$", message = "{user.username.pattern}")
    private String username;

    @NotBlank(message = "{user.fullName.not-blank}")
    @JsonDeserialize(using = StringOnlyDeserializer.class)
    @Size(min = 3, max = 150, message = "{user.fullName.size}")
    private String fullName;

    @JsonDeserialize(using = StringOnlyDeserializer.class)
    @Email(message = "{user.email.invalid}")
    @Size(max = 150, message = "{user.email.size}")
    private String email;

    @JsonDeserialize(using = StringOnlyDeserializer.class)
    @Size(min = 8, max = 72, message = "{user.password.size}")
    private String password;

    @NotEmpty(message = "{user.roleIds.not-empty}")
    @JsonDeserialize(using = IntegerListDeserializer.class)
    private List<Integer> roleIds = new ArrayList<>();

    @NotNull(message = "{common.active.not-null}")
    private Boolean active;
}
