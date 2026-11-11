package bo.cdr.app.apporder.domain.service.implement;

import bo.cdr.app.apporder.domain.event.OrderChangedEvent;
import bo.cdr.app.apporder.domain.service.interfaces.OrderService;
import bo.cdr.app.apporder.presentation.response.pojo.OrderEventPojo;
import bo.cdr.app.util.constants.WebSocketTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Envía los cambios de pedidos a /topic/orders SOLO después del commit: un rollback nunca llega a
 * las pantallas. Lee el estado ya confirmado en una transacción nueva (OrderService.getBoardPojo).
 * Un fallo de envío no afecta al pedido: el cliente se resincroniza con GET /order/board.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventPublisher {

    private final OrderService orderService;
    private final SimpMessagingTemplate messagingTemplate;
    private final Clock clock;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderChanged(OrderChangedEvent event) {
        try {
            OrderEventPojo payload = OrderEventPojo.of(event.type(), orderService.getBoardPojo(event.orderId()),
                    LocalDateTime.now(clock));
            messagingTemplate.convertAndSend(WebSocketTopics.ORDERS, payload);
        } catch (Exception e) {
            log.warn("No se pudo publicar {} del pedido {}: {}", event.type(), event.orderId(), e.getMessage());
        }
    }
}
