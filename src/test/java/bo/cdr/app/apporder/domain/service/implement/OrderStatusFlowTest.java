package bo.cdr.app.apporder.domain.service.implement;

import bo.cdr.app.appbase.domain.mapper.PagePojoMapper;
import bo.cdr.app.appcash.domain.entity.CashSession;
import bo.cdr.app.appcash.domain.service.interfaces.CashSessionService;
import bo.cdr.app.appcash.domain.service.interfaces.PaymentService;
import bo.cdr.app.appcatalog.domain.service.interfaces.ParameterService;
import bo.cdr.app.appcatalog.domain.service.interfaces.TableMarkerService;
import bo.cdr.app.apporder.data.OrderRepository;
import bo.cdr.app.apporder.data.OrderStatusHistoryRepository;
import bo.cdr.app.apporder.domain.entity.Order;
import bo.cdr.app.apporder.domain.entity.OrderStatusHistory;
import bo.cdr.app.apporder.domain.event.OrderChangedEvent;
import bo.cdr.app.apporder.domain.mapper.OrderMapper;
import bo.cdr.app.apporder.domain.model.OrderStateMachine;
import bo.cdr.app.apporder.domain.service.interfaces.OrderPricingService;
import bo.cdr.app.apporder.presentation.request.dto.OrderCancelDto;
import bo.cdr.app.apporder.presentation.request.dto.OrderStatusDto;
import bo.cdr.app.apporder.presentation.response.pojo.KitchenSettingsPojo;
import bo.cdr.app.appsecurity.auth.jwt.JwtService;
import bo.cdr.app.common.enums.OrderEventTypeEnum;
import bo.cdr.app.common.enums.OrderStatusEnum;
import bo.cdr.app.common.enums.ServiceTypeEnum;
import bo.cdr.app.exception.ConflictException;
import bo.cdr.app.exception.NotAllowedException;
import bo.cdr.app.exception.response.ValidationErrorException;
import bo.cdr.app.util.constants.ParameterCodes;
import bo.cdr.app.util.constants.RoleCodes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static bo.cdr.app.common.enums.OrderStatusEnum.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** Etapa 4: máquina de estados, cambios de estado con versión, cancelación con devolución. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderStatusFlowTest {

    @Nested
    class StateMachineTest {

        @ParameterizedTest(name = "{0} → {1} por {2}: {3}")
        @CsvSource({
                "PENDING, IN_PREPARATION, COCINA, true",
                "IN_PREPARATION, READY, COCINA, true",
                "READY, DELIVERED, CAJERO, true",
                "READY, DELIVERED, COCINA, false",
                "PENDING, IN_PREPARATION, CAJERO, false",
                // Decisión Etapa 4: retroceder un paso (COCINA, ADMIN)
                "READY, IN_PREPARATION, COCINA, true",
                "IN_PREPARATION, PENDING, COCINA, true",
                "READY, IN_PREPARATION, CAJERO, false",
                "READY, IN_PREPARATION, ADMIN, true",
                "PENDING, READY, ADMIN, false",
                "DELIVERED, READY, ADMIN, false",
                "CANCELLED, PENDING, ADMIN, false",
        })
        void transitions(OrderStatusEnum from, OrderStatusEnum to, String role, boolean expected) {
            assertThat(OrderStateMachine.isAllowed(from, to, List.of(role))).isEqualTo(expected);
        }

        @Test
        void cancelRules() {
            assertThat(OrderStateMachine.canCancel(PENDING, List.of(RoleCodes.CAJERO))).isTrue();
            assertThat(OrderStateMachine.canCancel(IN_PREPARATION, List.of(RoleCodes.CAJERO))).isFalse();
            assertThat(OrderStateMachine.canCancel(READY, List.of(RoleCodes.ADMIN))).isTrue();
            assertThat(OrderStateMachine.canCancel(DELIVERED, List.of(RoleCodes.ADMIN))).isFalse();
            assertThat(OrderStateMachine.canCancel(PENDING, List.of(RoleCodes.COCINA))).isFalse();
        }
    }

    @Nested
    class ServiceTest {
        @Mock OrderRepository orderRepository;
        @Mock OrderStatusHistoryRepository historyRepository;
        @Mock OrderPricingService pricingService;
        @Mock CashSessionService cashSessionService;
        @Mock PaymentService paymentService;
        @Mock TableMarkerService tableMarkerService;
        @Mock ParameterService parameterService;
        @Mock JwtService jwtService;
        @Mock ApplicationEventPublisher eventPublisher;

        final Clock clock = Clock.fixed(Instant.parse("2026-11-11T14:00:00Z"), ZoneId.of("UTC"));
        OrderServiceImpl service;
        Order order;

        @BeforeEach
        void setUp() {
            service = new OrderServiceImpl(orderRepository, historyRepository, new OrderMapper(), new PagePojoMapper(),
                    pricingService, cashSessionService, paymentService, tableMarkerService, parameterService, jwtService,
                    eventPublisher, clock);
            order = new Order();
            order.setId(40);
            order.setCode(3);
            order.setStatus(PENDING);
            order.setServiceType(ServiceTypeEnum.TAKEAWAY);
            order.setVersion(2L);
            order.setTotal(new BigDecimal("15.00"));
            order.setCashSession(new CashSession());
            when(orderRepository.findById(40)).thenReturn(Optional.of(order));
            when(jwtService.getCurrentUsername()).thenReturn("pepe.cocina");
            when(jwtService.getCurrentAuthorities()).thenReturn(List.of(RoleCodes.COCINA));
        }

        private OrderStatusDto status(String to, long version) {
            OrderStatusDto dto = new OrderStatusDto();
            dto.setToStatus(to);
            dto.setExpectedVersion(version);
            return dto;
        }

        private OrderCancelDto cancel(String reason, long version) {
            OrderCancelDto dto = new OrderCancelDto();
            dto.setReason(reason);
            dto.setExpectedVersion(version);
            return dto;
        }

        @Test
        void changeStatus_movesOrder_recordsHistory_andPublishesEvent() {
            service.changeStatus(40, status("IN_PREPARATION", 2));

            assertThat(order.getStatus()).isEqualTo(IN_PREPARATION);
            verify(orderRepository).saveAndFlush(order);
            ArgumentCaptor<OrderStatusHistory> history = ArgumentCaptor.forClass(OrderStatusHistory.class);
            verify(historyRepository).save(history.capture());
            assertThat(history.getValue().getFromStatus()).isEqualTo(PENDING);
            assertThat(history.getValue().getToStatus()).isEqualTo(IN_PREPARATION);
            assertThat(history.getValue().getChangedBy()).isEqualTo("pepe.cocina");
            verify(eventPublisher).publishEvent(new OrderChangedEvent(40, OrderEventTypeEnum.ORDER_STATUS_CHANGED));
        }

        @Test
        void changeStatus_returns409_whenVersionIsStale() {
            assertThatThrownBy(() -> service.changeStatus(40, status("IN_PREPARATION", 1)))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("#003");
            verify(orderRepository, never()).saveAndFlush(any());
            verifyNoInteractions(eventPublisher);
        }

        @Test
        void changeStatus_rejectsSkippingSteps() {
            assertThatThrownBy(() -> service.changeStatus(40, status("READY", 2)))
                    .isInstanceOf(ValidationErrorException.class);
        }

        @Test
        void changeStatus_rejectsRoleWithoutPermission() {
            order.setStatus(READY);
            assertThatThrownBy(() -> service.changeStatus(40, status("DELIVERED", 2)))
                    .isInstanceOf(NotAllowedException.class);
        }

        @Test
        void changeStatus_kitchenCanStepBack() {
            order.setStatus(READY);
            service.changeStatus(40, status("IN_PREPARATION", 2));
            assertThat(order.getStatus()).isEqualTo(IN_PREPARATION);
        }

        @Test
        void changeStatus_doesNotCancel() {
            assertThatThrownBy(() -> service.changeStatus(40, status("CANCELLED", 2)))
                    .isInstanceOf(ValidationErrorException.class)
                    .hasMessageContaining("Cancelar");
        }

        @Test
        void cancel_refundsInCancellerSession_andPublishesEvent() {
            when(jwtService.getCurrentAuthorities()).thenReturn(List.of(RoleCodes.CAJERO));
            when(jwtService.getCurrentUsername()).thenReturn("maria.caja");
            CashSession cancellerSession = new CashSession();
            cancellerSession.setId(8);
            when(cashSessionService.getOpenSessionOfCurrentUser()).thenReturn(cancellerSession);

            service.cancel(40, cancel("  Cliente se retiró ", 2));

            assertThat(order.getStatus()).isEqualTo(CANCELLED);
            assertThat(order.getCancelReason()).isEqualTo("Cliente se retiró");
            assertThat(order.getCancelledBy()).isEqualTo("maria.caja");
            assertThat(order.getCancelledAt()).isNotNull();
            verify(paymentService).refundOrderPayments(eq(40), eq("#003"), same(cancellerSession));
            verify(eventPublisher).publishEvent(new OrderChangedEvent(40, OrderEventTypeEnum.ORDER_CANCELLED));
        }

        @Test
        void cancel_cashierCannotCancelInPreparation() {
            order.setStatus(IN_PREPARATION);
            when(jwtService.getCurrentAuthorities()).thenReturn(List.of(RoleCodes.CAJERO));
            assertThatThrownBy(() -> service.cancel(40, cancel("x", 2))).isInstanceOf(NotAllowedException.class);
            verifyNoInteractions(paymentService);
        }

        @Test
        void cancel_adminCanCancelReady() {
            order.setStatus(READY);
            when(jwtService.getCurrentAuthorities()).thenReturn(List.of(RoleCodes.ADMIN));
            when(cashSessionService.getOpenSessionOfCurrentUser()).thenReturn(new CashSession());
            service.cancel(40, cancel("Error de cocina", 2));
            assertThat(order.getStatus()).isEqualTo(CANCELLED);
        }

        @Test
        void cancel_rejectsDeliveredOrder() {
            order.setStatus(DELIVERED);
            when(jwtService.getCurrentAuthorities()).thenReturn(List.of(RoleCodes.ADMIN));
            assertThatThrownBy(() -> service.cancel(40, cancel("x", 2))).isInstanceOf(ValidationErrorException.class);
        }

        @Test
        void cancel_requiresCancellerOpenCashSession() {
            when(jwtService.getCurrentAuthorities()).thenReturn(List.of(RoleCodes.CAJERO));
            when(cashSessionService.getOpenSessionOfCurrentUser())
                    .thenThrow(new ValidationErrorException("Debe abrir caja antes de registrar cobros o movimientos."));
            assertThatThrownBy(() -> service.cancel(40, cancel("x", 2))).isInstanceOf(ValidationErrorException.class);
            assertThat(order.getStatus()).isEqualTo(PENDING);
            verify(orderRepository, never()).saveAndFlush(any());
        }

        @Test
        void getBoard_defaultsToOpenStatuses() {
            when(orderRepository.findByStatusInOrderByCreatedAtAscIdAsc(any())).thenReturn(List.of(order));
            assertThat(service.getBoard(null)).hasSize(1);
            verify(orderRepository).findByStatusInOrderByCreatedAtAscIdAsc(
                    argThat(statuses -> statuses.containsAll(OrderStatusEnum.OPEN_STATUSES) && statuses.size() == 3));
        }

        @Test
        void kitchenSettings_readParameters() {
            when(parameterService.getDecimalValue(ParameterCodes.KITCHEN_DELAY_WARNING_MINUTES)).thenReturn(new BigDecimal("10"));
            when(parameterService.getDecimalValue(ParameterCodes.KITCHEN_DELAY_CRITICAL_MINUTES)).thenReturn(new BigDecimal("20"));
            assertThat(service.getKitchenSettings()).isEqualTo(new KitchenSettingsPojo(10, 20));
        }
    }
}
