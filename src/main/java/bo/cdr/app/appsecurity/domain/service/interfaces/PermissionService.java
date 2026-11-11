package bo.cdr.app.appsecurity.domain.service.interfaces;

import bo.cdr.app.appbase.domain.service.CRUDService;
import bo.cdr.app.appsecurity.domain.entity.Permission;
import bo.cdr.app.appsecurity.presentation.response.pojo.PermissionPojo;

import java.util.Collection;
import java.util.List;

/** Los permisos los genera PermissionSeeder (uno por endpoint); aquí solo se consultan. */
public interface PermissionService extends CRUDService<Permission, Integer> {

    boolean existsByCode(String code);

    List<PermissionPojo> getAllActive();

    /** Permisos activos con esos ids; falla si alguno no existe. */
    List<Permission> getActiveByIds(Collection<Integer> ids);
}
