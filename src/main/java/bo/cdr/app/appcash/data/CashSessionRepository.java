package bo.cdr.app.appcash.data;

import bo.cdr.app.appbase.data.repository.GenericRepository;
import bo.cdr.app.appcash.domain.entity.CashSession;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CashSessionRepository extends GenericRepository<CashSession, Integer> {

    @Query("""
        SELECT s FROM CashSession s JOIN FETCH s.user u
        WHERE u.id = :userId AND s.status = bo.cdr.app.common.enums.CashSessionStatusEnum.OPEN
    """)
    Optional<CashSession> findOpenByUserId(@Param("userId") Integer userId);
}
