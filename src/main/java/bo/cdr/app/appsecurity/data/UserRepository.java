package bo.cdr.app.appsecurity.data;

import bo.cdr.app.appbase.data.repository.GenericRepository;
import bo.cdr.app.appsecurity.domain.entity.UserApp;
import bo.cdr.app.appsecurity.presentation.response.pojo.UserAppPojo;
import bo.cdr.app.appsecurity.presentation.response.pojo.UserInfoPojo;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends GenericRepository<UserApp, Integer> {

    Optional<UserApp> findByUsernameIgnoreCase(String username);

    Optional<UserApp> findByUsernameIgnoreCaseAndActiveIsTrue(String username);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCaseAndIdNot(String username, Integer id);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Integer id);

    /** Igual que la referencia: permiso por método + URL normalizada a través de los roles del usuario. */
    @Query("""
    SELECT COUNT(u) > 0
    FROM UserApp u
    JOIN UserRole ur ON ur.user.id = u.id
    JOIN RolePermission rp ON rp.role.id = ur.role.id
    JOIN rp.permission p
    WHERE u.id = :userId
      AND u.active = true
      AND ur.role.active = true
      AND p.active = true
      AND p.urlWithoutIds LIKE :url
      AND p.httpMethod = :httpMethod
    """)
    boolean hasPermission(@Param("userId") Integer userId,
                          @Param("httpMethod") String httpMethod,
                          @Param("url") String url);

    @Query("""
    SELECT new bo.cdr.app.appsecurity.presentation.response.pojo.UserInfoPojo(u.id, u.username, u.fullName, u.email)
    FROM UserApp u
    WHERE u.id = :userId
    """)
    Optional<UserInfoPojo> getUserInfoById(@Param("userId") Integer userId);

    @Query("""
    SELECT COUNT(ur) > 0
    FROM UserRole ur
    WHERE ur.role.code = :roleCode
      AND ur.user.active = true
    """)
    boolean existsActiveUserWithRole(@Param("roleCode") String roleCode);

    /** Usuarios activos con el rol, sin contar a {@code excludedUserId} (regla "no quedarse sin ADMIN"). */
    @Query("""
    SELECT COUNT(DISTINCT ur.user.id)
    FROM UserRole ur
    WHERE ur.role.code = :roleCode
      AND ur.user.active = true
      AND ur.user.id <> :excludedUserId
    """)
    long countActiveUsersWithRoleExcluding(@Param("roleCode") String roleCode,
                                           @Param("excludedUserId") Integer excludedUserId);

    @Query("""
    SELECT new bo.cdr.app.appsecurity.presentation.response.pojo.UserAppPojo(
        u.id, u.username, u.fullName, u.email, u.active, u.createdBy, u.updatedBy, u.createdAt, u.updatedAt)
    FROM UserApp u
    ORDER BY u.fullName
    """)
    List<UserAppPojo> findAllUserAppPojo();

    @Query("""
    SELECT new bo.cdr.app.appsecurity.presentation.response.pojo.UserAppPojo(
        u.id, u.username, u.fullName, u.email, u.active, u.createdBy, u.updatedBy, u.createdAt, u.updatedAt)
    FROM UserApp u
    WHERE u.id = :id
    """)
    Optional<UserAppPojo> getUserAppPojoById(@Param("id") Integer id);
}
