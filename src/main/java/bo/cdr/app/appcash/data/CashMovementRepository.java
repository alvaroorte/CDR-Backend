package bo.cdr.app.appcash.data;

import bo.cdr.app.appbase.data.repository.GenericRepository;
import bo.cdr.app.appcash.domain.entity.CashMovement;
import bo.cdr.app.appcash.presentation.response.pojo.CashMovementPojo;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CashMovementRepository extends GenericRepository<CashMovement, Integer> {

    @Query("""
        SELECT new bo.cdr.app.appcash.presentation.response.pojo.CashMovementPojo(
            m.id, m.type, pm.id, pm.code, pm.name, m.amount, m.orderId, m.description, m.createdBy, m.createdAt)
        FROM CashMovement m LEFT JOIN m.paymentMethod pm
        WHERE m.cashSession.id = :cashSessionId
        ORDER BY m.id DESC
    """)
    List<CashMovementPojo> findPojoByCashSessionId(@Param("cashSessionId") Integer cashSessionId);
}
