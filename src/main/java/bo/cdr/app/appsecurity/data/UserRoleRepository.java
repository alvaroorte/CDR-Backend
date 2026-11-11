package bo.cdr.app.appsecurity.data;

import bo.cdr.app.appbase.data.repository.GenericRepository;
import bo.cdr.app.appsecurity.domain.entity.Role;
import bo.cdr.app.appsecurity.domain.entity.UserApp;
import bo.cdr.app.appsecurity.domain.entity.UserRole;
import bo.cdr.app.appsecurity.presentation.response.pojo.RolePojo;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Un rol desactivado deja de otorgarse: todas las consultas de roles efectivos filtran r.active. */
@Repository
public interface UserRoleRepository extends GenericRepository<UserRole, Integer> {

    @Query("SELECT new bo.cdr.app.appsecurity.presentation.response.pojo.RolePojo(r.id, r.name, r.code) " +
            " FROM UserRole ur " +
            " JOIN ur.role r " +
            " WHERE ur.user.id = :userId AND r.active = true ")
    List<RolePojo> getRolesByUserId(@Param("userId") Integer userId);

    /** Para el listado de usuarios: incluye roles inactivos (el ADMIN debe verlos para corregirlos). */
    @Query("SELECT new bo.cdr.app.appsecurity.presentation.response.pojo.RolePojo(ur.user.id, r.id, r.name, r.code) " +
            " FROM UserRole ur " +
            " JOIN ur.role r " +
            " WHERE ur.user.id IN :userIds ")
    List<RolePojo> getRolesByUserIds(@Param("userIds") Collection<Integer> userIds);

    // JOIN FETCH: el rol es LAZY y open-in-view está desactivado; se carga en la misma consulta.
    @Query("SELECT ur FROM UserRole ur JOIN FETCH ur.role r WHERE ur.user = :user AND r.active = true")
    List<UserRole> findByUser(@Param("user") UserApp user);

    boolean existsByUserAndRole(UserApp userApp, Role role);

    Optional<UserRole> findByUserAndRole(UserApp user, Role role);

    boolean existsByRoleIdAndUserActiveIsTrue(Integer roleId);

    /** Tabla puente sin historia: se reemplaza por completo al reasignar roles. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM UserRole ur WHERE ur.user.id = :userId")
    void deleteByUserId(@Param("userId") Integer userId);
}
