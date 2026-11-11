package bo.cdr.app.appsecurity.presentation.request.dto;

import bo.cdr.app.config.IntegerListDeserializer;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.util.ArrayList;
import java.util.List;

/** Reemplaza el conjunto completo de permisos del rol (lista vacía = sin permisos). */
@Data
@NoArgsConstructor
public class RolePermissionsDto {

    @NotNull(message = "{role.permissionIds.not-null}")
    @JsonDeserialize(using = IntegerListDeserializer.class)
    private List<Integer> permissionIds = new ArrayList<>();
}
