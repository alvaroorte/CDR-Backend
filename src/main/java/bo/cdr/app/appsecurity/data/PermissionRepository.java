package bo.cdr.app.appsecurity.data;

import bo.cdr.app.appbase.data.repository.GenericRepository;
import bo.cdr.app.appsecurity.domain.entity.Permission;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PermissionRepository extends GenericRepository<Permission, Integer> {

    boolean existsByCode(String code);

    Optional<Permission> findByCode(String code);
}
