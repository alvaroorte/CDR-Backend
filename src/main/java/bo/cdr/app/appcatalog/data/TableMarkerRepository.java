package bo.cdr.app.appcatalog.data;

import bo.cdr.app.appbase.data.repository.GenericRepository;
import bo.cdr.app.appcatalog.domain.entity.TableMarker;
import bo.cdr.app.appcatalog.presentation.response.pojo.TableMarkerOccupancyProjection;
import bo.cdr.app.appcatalog.presentation.response.pojo.TableMarkerPojo;
import bo.cdr.app.common.enums.OrderStatusEnum;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface TableMarkerRepository extends GenericRepository<TableMarker, Integer> {

    boolean existsByNumberAndActiveIsTrue(Integer number);

    boolean existsByNumberAndActiveIsTrueAndIdNot(Integer number, Integer id);

    @Query("""
        SELECT new bo.cdr.app.appcatalog.presentation.response.pojo.TableMarkerPojo(
            t.id, t.number, t.description, t.active, t.createdBy, t.updatedBy, t.createdAt, t.updatedAt)
        FROM TableMarker t
        ORDER BY t.number
    """)
    List<TableMarkerPojo> findAllTableMarkerPojo();

    @Query("""
        SELECT new bo.cdr.app.appcatalog.presentation.response.pojo.TableMarkerPojo(
            t.id, t.number, t.description, t.active, t.createdBy, t.updatedBy, t.createdAt, t.updatedAt)
        FROM TableMarker t WHERE t.id = :id
    """)
    Optional<TableMarkerPojo> getTableMarkerPojoById(@Param("id") Integer id);

    /**
     * Adornos activos con su pedido abierto, si lo tienen. La ocupación se deriva de los pedidos
     * (mismos estados que el índice ux_orders_marker_open); no hay estado manual.
     */
    @Query("""
        SELECT new bo.cdr.app.appcatalog.presentation.response.pojo.TableMarkerOccupancyProjection(
            t.id, t.number, t.description, o.id, o.code)
        FROM TableMarker t
        LEFT JOIN bo.cdr.app.apporder.domain.entity.Order o
            ON o.tableMarker = t AND o.status IN :openStatuses
        WHERE t.active = true
        ORDER BY t.number
    """)
    List<TableMarkerOccupancyProjection> findActiveWithOpenOrder(
            @Param("openStatuses") Collection<OrderStatusEnum> openStatuses);
}
