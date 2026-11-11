package bo.cdr.app.appcash.data;

import bo.cdr.app.appbase.data.repository.GenericRepository;
import bo.cdr.app.appcash.domain.entity.Payment;
import bo.cdr.app.appcash.presentation.response.pojo.PaymentPojo;
import bo.cdr.app.common.enums.PaymentStatusEnum;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends GenericRepository<Payment, Integer> {

    List<Payment> findByOrderIdAndStatusOrderByIdAsc(Integer orderId, PaymentStatusEnum status);

    @Query("""
        SELECT new bo.cdr.app.appcash.presentation.response.pojo.PaymentPojo(
            p.id, pm.id, pm.code, pm.name, p.amount, p.reference, p.status, p.createdBy, p.createdAt)
        FROM Payment p JOIN p.paymentMethod pm
        WHERE p.orderId = :orderId
        ORDER BY p.id
    """)
    List<PaymentPojo> findPojoByOrderId(@Param("orderId") Integer orderId);
}
