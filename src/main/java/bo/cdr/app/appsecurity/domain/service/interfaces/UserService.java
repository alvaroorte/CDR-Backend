package bo.cdr.app.appsecurity.domain.service.interfaces;

import bo.cdr.app.appbase.domain.service.CRUDService;
import bo.cdr.app.appsecurity.domain.entity.UserApp;
import bo.cdr.app.appsecurity.presentation.request.dto.UserAppDto;
import bo.cdr.app.appsecurity.presentation.response.pojo.UserAppPojo;
import bo.cdr.app.appsecurity.presentation.response.pojo.UserInfoPojo;

import java.util.List;
import java.util.Optional;

public interface UserService extends CRUDService<UserApp, Integer> {

    /** Usuario activo por username. Vacío si no existe o fue desactivado. */
    Optional<UserApp> findActiveByUsername(String username);

    /** Datos y roles del usuario autenticado. */
    UserInfoPojo getCurrentUserInfo();

    boolean hasPermission(Integer userId, String method, String url);

    List<UserAppPojo> getAllUserAppPojo();

    UserAppPojo getUserAppPojoById(Integer id);

    UserAppPojo create(UserAppDto dto);

    UserAppPojo update(Integer id, UserAppDto dto);

    /** Restablecimiento por un ADMIN; revoca las sesiones abiertas del usuario. */
    void resetPassword(Integer id, String newPassword);
}
