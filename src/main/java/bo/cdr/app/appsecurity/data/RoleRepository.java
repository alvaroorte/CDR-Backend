package bo.cdr.app.appsecurity.data;

import bo.cdr.app.appbase.data.repository.GenericRepository;
import bo.cdr.app.appsecurity.domain.entity.Role;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Set;
import java.util.Optional;



public interface RoleRepository extends GenericRepository<Role, Integer> {

    Optional<Role> findByCode(String code);

    boolean existsByName(String name);

    boolean existsByCode(String code);

    boolean existsByNameAndIdNot(String name, Integer id);

    @Query("""
    SELECT r FROM Role r
    WHERE r.id IN :ids
      AND r.active = true
    """)
    List<Role> getAllByIds(Set<Integer> ids);
}
