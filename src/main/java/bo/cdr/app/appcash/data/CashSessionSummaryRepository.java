package bo.cdr.app.appcash.data;

import bo.cdr.app.appbase.data.repository.GenericRepository;
import bo.cdr.app.appcash.domain.entity.CashSessionSummary;
import bo.cdr.app.appcash.presentation.response.pojo.CashSummaryPojo;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface CashSessionSummaryRepository extends GenericRepository<CashSessionSummary, Integer> {

    @Query("""
        SELECT new bo.cdr.app.appcash.presentation.response.pojo.CashSummaryPojo(
            s.cashSession.id, pm.id, pm.code, pm.name, s.expectedAmount, s.declaredAmount, s.difference)
        FROM CashSessionSummary s JOIN s.paymentMethod pm
        WHERE s.cashSession.id IN :cashSessionIds
        ORDER BY s.cashSession.id, s.id
    """)
    List<CashSummaryPojo> findPojoByCashSessionIds(@Param("cashSessionIds") Collection<Integer> cashSessionIds);
}
