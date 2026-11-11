package bo.cdr.app.apporder.data;

import bo.cdr.app.appbase.data.repository.GenericRepository;
import bo.cdr.app.apporder.domain.entity.Order;
import bo.cdr.app.common.enums.OrderStatusEnum;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Repository
public interface OrderRepository extends GenericRepository<Order, Integer> {

    /** Tablero: pedidos en esos estados, del más antiguo al más reciente (detalle por batch fetch). */
    List<Order> findByStatusInOrderByCreatedAtAscIdAsc(Collection<OrderStatusEnum> statuses);

    boolean existsByTableMarkerIdAndStatusIn(Integer tableMarkerId, Collection<OrderStatusEnum> statuses);

    /**
     * Siguiente código del día, atómico: el UPSERT bloquea la fila del día hasta el commit, de modo
     * que dos cajeros simultáneos obtienen códigos distintos (y un rollback no deja huecos visibles
     * a otros, porque la fila sigue bloqueada hasta entonces).
     */
    @Query(value = """
        INSERT INTO order_daily_sequences (business_date, last_value) VALUES (:businessDate, 1)
        ON CONFLICT (business_date) DO UPDATE SET last_value = order_daily_sequences.last_value + 1
        RETURNING last_value
    """, nativeQuery = true)
    Integer nextDailyCode(@Param("businessDate") LocalDate businessDate);
}
