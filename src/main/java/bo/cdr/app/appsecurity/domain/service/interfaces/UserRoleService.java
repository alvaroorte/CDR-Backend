package bo.cdr.app.appsecurity.domain.service.interfaces;

import bo.cdr.app.appbase.domain.service.CRUDService;
import bo.cdr.app.appsecurity.domain.entity.Role;
import bo.cdr.app.appsecurity.domain.entity.UserApp;
import bo.cdr.app.appsecurity.domain.entity.UserRole;
import bo.cdr.app.appsecurity.presentation.response.pojo.RolePojo;

import java.util.Collection;
import java.util.List;

public interface UserRoleService extends CRUDService<UserRole, Integer> {

    List<RolePojo> getRolesByUserId(Integer userId);

    List<RolePojo> getRolesByUserIds(Collection<Integer> userIds);
    List<UserRole> getRolesByUser(UserApp user);
    boolean existsByUserAndRole(UserApp user, Role role);
    UserRole create(UserApp user, Role role);
    void deleteUserRole(UserApp user, Role role);

    /** Reemplaza todos los roles del usuario. */
    void replaceRoles(UserApp user, java.util.Collection<Role> roles);

    /** ¿Algún usuario activo tiene este rol? */
    boolean isAssignedToActiveUsers(Integer roleId);
}
