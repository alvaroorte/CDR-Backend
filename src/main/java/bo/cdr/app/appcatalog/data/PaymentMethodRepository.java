package bo.cdr.app.appcatalog.data;

import bo.cdr.app.appbase.data.repository.GenericRepository;
import bo.cdr.app.appcatalog.domain.entity.PaymentMethod;
import bo.cdr.app.appcatalog.presentation.response.pojo.PaymentMethodPojo;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentMethodRepository extends GenericRepository<PaymentMethod, Integer> {

    boolean existsByCodeIgnoreCase(String code);

    Optional<PaymentMethod> findByCodeIgnoreCase(String code);

    boolean existsByNameIgnoreCaseAndActiveIsTrue(String name);

    boolean existsByNameIgnoreCaseAndActiveIsTrueAndIdNot(String name, Integer id);

    @Query("""
        SELECT new bo.cdr.app.appcatalog.presentation.response.pojo.PaymentMethodPojo(
            m.id, m.code, m.name, m.requiresReference, m.givesChange, m.active,
            m.createdBy, m.updatedBy, m.createdAt, m.updatedAt)
        FROM PaymentMethod m
        ORDER BY m.name
    """)
    List<PaymentMethodPojo> findAllPaymentMethodPojo();

    @Query("""
        SELECT new bo.cdr.app.appcatalog.presentation.response.pojo.PaymentMethodPojo(
            m.id, m.code, m.name, m.requiresReference, m.givesChange, m.active,
            m.createdBy, m.updatedBy, m.createdAt, m.updatedAt)
        FROM PaymentMethod m WHERE m.id = :id
    """)
    Optional<PaymentMethodPojo> getPaymentMethodPojoById(@Param("id") Integer id);

    List<PaymentMethod> findAllByActiveIsTrueOrderByNameAsc();
}
