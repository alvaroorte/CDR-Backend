package bo.cdr.app.appsecurity.domain.service;

import bo.cdr.app.appbase.domain.service.CRUDService;
import bo.cdr.app.appsecurity.domain.entity.Permission;

/** Etapa 1: lo que usa PermissionSeeder. La gestión de permisos (pantalla de roles) llega en la Etapa 2. */
public interface PermissionService extends CRUDService<Permission, Integer> {

    boolean existsByCode(String code);
}
