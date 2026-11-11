package bo.cdr.app.appcatalog.data;

import bo.cdr.app.appbase.data.repository.GenericRepository;
import bo.cdr.app.appcatalog.domain.entity.Parameter;
import bo.cdr.app.appcatalog.presentation.response.pojo.ParameterPojo;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParameterRepository extends GenericRepository<Parameter, Integer> {

    Optional<Parameter> findByCodeAndActiveIsTrue(String code);

    @Query("""
        SELECT new bo.cdr.app.appcatalog.presentation.response.pojo.ParameterPojo(
            p.id, p.folder, p.code, p.name, p.value, p.valueType, p.description, p.editable, p.active,
            p.createdBy, p.updatedBy, p.createdAt, p.updatedAt)
        FROM Parameter p
        ORDER BY p.folder, p.name
    """)
    List<ParameterPojo> findAllParameterPojo();

    @Query("""
        SELECT new bo.cdr.app.appcatalog.presentation.response.pojo.ParameterPojo(
            p.id, p.folder, p.code, p.name, p.value, p.valueType, p.description, p.editable, p.active,
            p.createdBy, p.updatedBy, p.createdAt, p.updatedAt)
        FROM Parameter p WHERE p.id = :id
    """)
    Optional<ParameterPojo> getParameterPojoById(@Param("id") Integer id);
}
