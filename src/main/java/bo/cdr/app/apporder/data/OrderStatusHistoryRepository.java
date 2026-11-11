package bo.cdr.app.apporder.data;

import bo.cdr.app.appbase.data.repository.GenericRepository;
import bo.cdr.app.apporder.domain.entity.OrderStatusHistory;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderStatusHistoryRepository extends GenericRepository<OrderStatusHistory, Integer> {

    List<OrderStatusHistory> findByOrderIdOrderByIdAsc(Integer orderId);
}
