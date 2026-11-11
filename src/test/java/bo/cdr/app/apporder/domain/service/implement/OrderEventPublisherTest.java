package bo.cdr.app.apporder.domain.service.implement;

import bo.cdr.app.apporder.domain.event.OrderChangedEvent;
import bo.cdr.app.apporder.domain.service.interfaces.OrderService;
import bo.cdr.app.apporder.presentation.response.pojo.OrderBoardPojo;
import bo.cdr.app.apporder.presentation.response.pojo.OrderEventPojo;
import bo.cdr.app.common.enums.OrderEventTypeEnum;
import bo.cdr.app.common.enums.OrderStatusEnum;
import bo.cdr.app.common.enums.ServiceTypeEnum;
import bo.cdr.app.util.constants.WebSocketTopics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderEventPublisherTest {

    @Mock OrderService orderService;
    @Mock SimpMessagingTemplate messagingTemplate;

    @Test
    void sendsCommittedStateWithVersion_toOrdersTopic() {
        OrderBoardPojo board = new OrderBoardPojo(40, 3, "#003", ServiceTypeEnum.DINE_IN, "En el local", 5, null,
                OrderStatusEnum.READY, "LISTO", new BigDecimal("15.00"), 4L, null, "maria.caja", null, List.of());
        when(orderService.getBoardPojo(40)).thenReturn(board);

        new OrderEventPublisher(orderService, messagingTemplate, Clock.systemUTC())
                .onOrderChanged(new OrderChangedEvent(40, OrderEventTypeEnum.ORDER_STATUS_CHANGED));

        ArgumentCaptor<OrderEventPojo> sent = ArgumentCaptor.forClass(OrderEventPojo.class);
        verify(messagingTemplate).convertAndSend(eq(WebSocketTopics.ORDERS), sent.capture());
        assertThat(sent.getValue().version()).isEqualTo(4L);
        assertThat(sent.getValue().status()).isEqualTo(OrderStatusEnum.READY);
        assertThat(sent.getValue().type()).isEqualTo(OrderEventTypeEnum.ORDER_STATUS_CHANGED);
        assertThat(sent.getValue().eventId()).isNotBlank();
    }

    @Test
    void sendFailure_doesNotPropagate() {
        when(orderService.getBoardPojo(40)).thenThrow(new RuntimeException("broker caído"));
        new OrderEventPublisher(orderService, messagingTemplate, Clock.systemUTC())
                .onOrderChanged(new OrderChangedEvent(40, OrderEventTypeEnum.ORDER_CREATED));
        verify(messagingTemplate, never()).convertAndSend(any(String.class), any(Object.class));
    }
}
