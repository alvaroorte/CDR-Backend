package bo.cdr.app.appsecurity.domain.mapper;

import bo.cdr.app.appsecurity.domain.entity.Role;
import bo.cdr.app.appsecurity.presentation.request.dto.RoleDto;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class RoleMapper {
    public Role fromDto(RoleDto dto, Role roleFound) {
        Role role = roleFound != null ? roleFound : new Role();
        if (roleFound == null) {
            role.setCode(dto.getCode().trim().toUpperCase(Locale.ROOT)); // el código no cambia al editar
        }
        role.setName(dto.getName().trim());
        role.setDescription(dto.getDescription());
        role.setActive(dto.getActive());
        return role;
    }
}
