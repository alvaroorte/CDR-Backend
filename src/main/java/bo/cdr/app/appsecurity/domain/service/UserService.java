package bo.cdr.app.appsecurity.domain.service;

import bo.cdr.app.appbase.domain.service.CRUDService;
import bo.cdr.app.appsecurity.domain.entity.UserApp;
import bo.cdr.app.appsecurity.presentation.response.pojo.UserInfoPojo;

import java.util.Optional;

/**
 * Etapa 1: solo lo necesario para autenticación y autorización.
 * El CRUD de usuarios (alta, edición, activación, roles) se agrega en la Etapa 2.
 */
public interface UserService extends CRUDService<UserApp, Integer> {

    /** Usuario activo por username. Vacío si no existe o fue desactivado. */
    Optional<UserApp> findActiveByUsername(String username);

    /** Datos y roles del usuario autenticado. */
    UserInfoPojo getCurrentUserInfo();

    boolean hasPermission(Integer userId, String method, String url);
}
