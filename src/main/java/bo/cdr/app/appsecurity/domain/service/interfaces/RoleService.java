package bo.cdr.app.appsecurity.domain.service.interfaces;

import bo.cdr.app.appbase.domain.service.CRUDService;
import bo.cdr.app.appsecurity.domain.entity.Role;
import bo.cdr.app.appsecurity.presentation.request.dto.RoleDto;
import bo.cdr.app.appsecurity.presentation.response.pojo.RolePojo;

import java.util.Collection;
import java.util.List;

public interface RoleService extends CRUDService<Role, Integer> {
    RolePojo create(RoleDto dto);
    RolePojo update(Integer id, RoleDto dto);
    List<RolePojo> getAllRolePojo();
    RolePojo getRolePojoById(Integer id);
    List<RolePojo> getSelect();
    /** Roles activos con esos ids; falla si alguno no existe o está inactivo. */
    List<Role> getActiveByIds(Collection<Integer> ids);
    List<Integer> getPermissionIds(Integer roleId);
    List<Integer> replacePermissions(Integer roleId, Collection<Integer> permissionIds);
}
