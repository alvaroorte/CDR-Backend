package bo.cdr.app.apporder.domain.service.interfaces;

import bo.cdr.app.appbase.domain.service.CRUDService;
import bo.cdr.app.appbase.presentation.response.PagePojo;
import bo.cdr.app.apporder.domain.entity.Order;
import bo.cdr.app.apporder.presentation.request.dto.OrderCancelDto;
import bo.cdr.app.apporder.presentation.request.dto.OrderDto;
import bo.cdr.app.apporder.presentation.request.dto.OrderQuoteDto;
import bo.cdr.app.apporder.presentation.request.dto.OrderStatusDto;
import bo.cdr.app.apporder.presentation.request.filter.OrderFilter;
import bo.cdr.app.apporder.presentation.response.pojo.KitchenSettingsPojo;
import bo.cdr.app.apporder.presentation.response.pojo.OrderBoardPojo;
import bo.cdr.app.apporder.presentation.response.pojo.OrderListPojo;
import bo.cdr.app.apporder.presentation.response.pojo.OrderPojo;
import bo.cdr.app.apporder.presentation.response.pojo.OrderQuotePojo;
import bo.cdr.app.common.enums.OrderStatusEnum;

import java.util.List;

public interface OrderService extends CRUDService<Order, Integer> {

    /** Mismo cálculo que el registro, sin persistir. */
    OrderQuotePojo quote(OrderQuoteDto dto);

    /**
     * Registra pedido + detalle + historial + pagos + movimientos SALE en una sola transacción.
     * Exige caja abierta del usuario; en DINE_IN, adorno activo y libre. Publica ORDER_CREATED tras el commit.
     */
    OrderPojo create(OrderDto dto);

    /** Detalle con pagos e historial de estados. */
    OrderPojo getOrderPojoById(Integer id);

    /** Tablero (fuente de verdad al conectar/reconectar el WebSocket). Sin estados = abiertos. */
    List<OrderBoardPojo> getBoard(List<OrderStatusEnum> statuses);

    /** Estado confirmado de un pedido para el evento WebSocket (transacción propia, tras el commit). */
    OrderBoardPojo getBoardPojo(Integer id);

    /** Cambio de estado según OrderStateMachine y el rol; 409 si {@code expectedVersion} no coincide. */
    OrderPojo changeStatus(Integer id, OrderStatusDto dto);

    /** Cancelación con motivo y devolución en la caja abierta de quien cancela. */
    OrderPojo cancel(Integer id, OrderCancelDto dto);

    PagePojo<OrderListPojo> pageable(int page, int size, String sortBy, String sortOrder, OrderFilter filter);

    KitchenSettingsPojo getKitchenSettings();
}
