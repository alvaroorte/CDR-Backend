package bo.cdr.app.appsecurity.data;

import bo.cdr.app.appbase.data.repository.GenericRepository;
import bo.cdr.app.appsecurity.domain.entity.Role;
import bo.cdr.app.appsecurity.presentation.response.pojo.RolePojo;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends GenericRepository<Role, Integer> {

    Optional<Role> findByCode(String code);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Integer id);

    List<Role> findAllByIdInAndActiveIsTrue(Collection<Integer> ids);

    @Query("""
        SELECT new bo.cdr.app.appsecurity.presentation.response.pojo.RolePojo(
            r.id, r.name, r.code, r.description, r.active, r.createdBy, r.updatedBy, r.createdAt, r.updatedAt)
        FROM Role r
        ORDER BY r.name
    """)
    List<RolePojo> findAllRolePojo();

    @Query("""
        SELECT new bo.cdr.app.appsecurity.presentation.response.pojo.RolePojo(
            r.id, r.name, r.code, r.description, r.active, r.createdBy, r.updatedBy, r.createdAt, r.updatedAt)
        FROM Role r WHERE r.id = :id
    """)
    Optional<RolePojo> getRolePojoById(@Param("id") Integer id);

    @Query("""
        SELECT new bo.cdr.app.appsecurity.presentation.response.pojo.RolePojo(r.id, r.name, r.code)
        FROM Role r WHERE r.active = true
        ORDER BY r.name
    """)
    List<RolePojo> findSelect();
}
