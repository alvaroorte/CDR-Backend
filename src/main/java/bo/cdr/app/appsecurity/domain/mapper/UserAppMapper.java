package bo.cdr.app.appsecurity.domain.mapper;

import bo.cdr.app.appsecurity.domain.entity.UserApp;
import bo.cdr.app.appsecurity.presentation.request.dto.UserAppDto;
import org.springframework.stereotype.Component;

/** La contraseña NO se mapea aquí: el servicio la cifra (BCrypt) solo al crear o restablecer. */
@Component
public class UserAppMapper {
    public UserApp fromDto(UserAppDto dto, UserApp userFound) {
        UserApp user = userFound != null ? userFound : new UserApp();
        user.setUsername(dto.getUsername().trim());
        user.setFullName(dto.getFullName().trim());
        user.setEmail(dto.getEmail() == null || dto.getEmail().isBlank() ? null : dto.getEmail().trim());
        user.setActive(dto.getActive());
        return user;
    }
}
