package bo.cdr.app.appcatalog.data;

import bo.cdr.app.appbase.data.repository.GenericRepository;
import bo.cdr.app.appcatalog.domain.entity.Addon;
import bo.cdr.app.appcatalog.presentation.response.pojo.AddonPojo;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface AddonRepository extends GenericRepository<Addon, Integer> {

    boolean existsByNameIgnoreCaseAndActiveIsTrue(String name);

    boolean existsByNameIgnoreCaseAndActiveIsTrueAndIdNot(String name, Integer id);

    @Query("""
        SELECT new bo.cdr.app.appcatalog.presentation.response.pojo.AddonPojo(
            a.id, a.name, a.price, a.active, a.createdBy, a.updatedBy, a.createdAt, a.updatedAt)
        FROM Addon a
        ORDER BY a.name
    """)
    List<AddonPojo> findAllAddonPojo();

    @Query("""
        SELECT new bo.cdr.app.appcatalog.presentation.response.pojo.AddonPojo(
            a.id, a.name, a.price, a.active, a.createdBy, a.updatedBy, a.createdAt, a.updatedAt)
        FROM Addon a WHERE a.id = :id
    """)
    Optional<AddonPojo> getAddonPojoById(@Param("id") Integer id);

    @Query("""
        SELECT new bo.cdr.app.appcatalog.presentation.response.pojo.AddonPojo(
            a.id, a.name, a.price, a.active, a.createdBy, a.updatedBy, a.createdAt, a.updatedAt)
        FROM Addon a WHERE a.active = true
        ORDER BY a.name
    """)
    List<AddonPojo> findActiveAddonPojo();

    List<Addon> findAllByIdInAndActiveIsTrue(Collection<Integer> ids);
}
