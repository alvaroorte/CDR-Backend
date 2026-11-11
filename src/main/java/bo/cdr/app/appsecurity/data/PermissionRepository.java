package bo.cdr.app.appsecurity.data;

import bo.cdr.app.appbase.data.repository.GenericRepository;
import bo.cdr.app.appsecurity.domain.entity.Permission;
import bo.cdr.app.appsecurity.presentation.response.pojo.PermissionPojo;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PermissionRepository extends GenericRepository<Permission, Integer> {

    boolean existsByCode(String code);

    Optional<Permission> findByCode(String code);

    List<Permission> findAllByIdInAndActiveIsTrue(Collection<Integer> ids);

    @Query("""
        SELECT new bo.cdr.app.appsecurity.presentation.response.pojo.PermissionPojo(
            p.id, p.code, p.name, p.description, p.httpMethod, p.url)
        FROM Permission p
        WHERE p.active = true
        ORDER BY p.url, p.httpMethod
    """)
    List<PermissionPojo> findAllActivePermissionPojo();
}
