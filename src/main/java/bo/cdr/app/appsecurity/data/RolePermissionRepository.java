package bo.cdr.app.appsecurity.data;

import bo.cdr.app.appbase.data.repository.GenericRepository;
import bo.cdr.app.appsecurity.domain.entity.RolePermission;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RolePermissionRepository extends GenericRepository<RolePermission, Integer> {

    @Query("SELECT rp.permission.id FROM RolePermission rp WHERE rp.role.id = :roleId ORDER BY rp.permission.id")
    List<Integer> findPermissionIdsByRoleId(@Param("roleId") Integer roleId);

    /** Tabla puente sin historia: se reemplaza por completo al reasignar permisos. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM RolePermission rp WHERE rp.role.id = :roleId")
    void deleteByRoleId(@Param("roleId") Integer roleId);
}
