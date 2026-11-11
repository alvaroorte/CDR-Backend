package bo.cdr.app.appcash.data;

import bo.cdr.app.appbase.data.repository.GenericRepository;
import bo.cdr.app.appcash.domain.entity.CashSession;
import bo.cdr.app.appcash.presentation.response.pojo.CashOpenOrderPojo;
import bo.cdr.app.common.enums.OrderStatusEnum;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface CashSessionRepository extends GenericRepository<CashSession, Integer> {

    @Query("""
        SELECT s FROM CashSession s JOIN FETCH s.user u
        WHERE u.id = :userId AND s.status = bo.cdr.app.common.enums.CashSessionStatusEnum.OPEN
    """)
    Optional<CashSession> findOpenByUserId(@Param("userId") Integer userId);

    /**
     * Caja abierta para registrar dinero (cobro, devolución, movimiento): bloquea la fila (FOR UPDATE)
     * e incrementa su @Version. Los cobros del mismo cajero se serializan sin fallar; un cobro que
     * espera a un cierre ya no encuentra la caja abierta; y un arqueo hecho antes de un cobro queda
     * con versión vieja (409 al cerrar).
     */
    @Lock(LockModeType.PESSIMISTIC_FORCE_INCREMENT)
    @Query("""
        SELECT s FROM CashSession s
        WHERE s.user.id = :userId AND s.status = bo.cdr.app.common.enums.CashSessionStatusEnum.OPEN
    """)
    Optional<CashSession> findOpenByUserIdForUpdate(@Param("userId") Integer userId);

    /** Caja a cerrar, bloqueada (FOR UPDATE) para que ningún cobro entre mientras se guarda el arqueo. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM CashSession s WHERE s.id = :id")
    Optional<CashSession> findByIdForUpdate(@Param("id") Integer id);

    /** Cajas abiertas de todos los usuarios (ADMIN: detectar cajas olvidadas). */
    @Query("""
        SELECT s FROM CashSession s JOIN FETCH s.user u
        WHERE s.status = bo.cdr.app.common.enums.CashSessionStatusEnum.OPEN
        ORDER BY s.openedAt
    """)
    List<CashSession> findAllOpen();

    /** Pedidos cobrados en esta caja que aún no se entregaron (advertencia al cerrar). */
    @Query("""
        SELECT new bo.cdr.app.appcash.presentation.response.pojo.CashOpenOrderPojo(o.id, o.code, o.status)
        FROM bo.cdr.app.apporder.domain.entity.Order o
        WHERE o.cashSession.id = :cashSessionId AND o.status IN :statuses
        ORDER BY o.id
    """)
    List<CashOpenOrderPojo> findOrdersInStatuses(@Param("cashSessionId") Integer cashSessionId,
                                                 @Param("statuses") Collection<OrderStatusEnum> statuses);
}
