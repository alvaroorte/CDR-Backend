package bo.cdr.app.apporder.domain.service.implement;

import bo.cdr.app.appcash.domain.entity.CashSession;
import bo.cdr.app.appcash.domain.model.ResolvedPayment;
import bo.cdr.app.appcash.domain.service.interfaces.CashSessionService;
import bo.cdr.app.appcash.domain.service.interfaces.PaymentService;
import bo.cdr.app.appcash.presentation.request.dto.PaymentDto;
import bo.cdr.app.appcatalog.domain.entity.Addon;
import bo.cdr.app.appcatalog.domain.entity.Category;
import bo.cdr.app.appcatalog.domain.entity.PaymentMethod;
import bo.cdr.app.appcatalog.domain.entity.Product;
import bo.cdr.app.appcatalog.domain.entity.TableMarker;
import bo.cdr.app.appcatalog.domain.service.interfaces.TableMarkerService;
import bo.cdr.app.apporder.data.OrderRepository;
import bo.cdr.app.apporder.data.OrderStatusHistoryRepository;
import bo.cdr.app.apporder.domain.entity.Order;
import bo.cdr.app.apporder.domain.entity.OrderStatusHistory;
import bo.cdr.app.apporder.domain.mapper.OrderMapper;
import bo.cdr.app.apporder.domain.model.PricedOrder;
import bo.cdr.app.apporder.domain.service.interfaces.OrderPricingService;
import bo.cdr.app.apporder.presentation.request.dto.OrderDto;
import bo.cdr.app.apporder.presentation.response.pojo.OrderPojo;
import bo.cdr.app.appsecurity.auth.jwt.JwtService;
import bo.cdr.app.common.enums.DiscountTypeEnum;
import bo.cdr.app.common.enums.OrderStatusEnum;
import bo.cdr.app.common.enums.ServiceTypeEnum;
import bo.cdr.app.exception.ConflictException;
import bo.cdr.app.exception.response.ValidationErrorException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.context.ApplicationEventPublisher;
import bo.cdr.app.appbase.domain.mapper.PagePojoMapper;
import bo.cdr.app.appcatalog.domain.service.interfaces.ParameterService;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderServiceImplTest {

    @Mock OrderRepository orderRepository;
    @Mock OrderStatusHistoryRepository historyRepository;
    @Mock OrderPricingService pricingService;
    @Mock CashSessionService cashSessionService;
    @Mock PaymentService paymentService;
    @Mock TableMarkerService tableMarkerService;
    @Mock JwtService jwtService;
    @Mock ParameterService parameterService;
    @Mock ApplicationEventPublisher eventPublisher;

    // 2026-11-11 01:30 UTC = 2026-11-10 21:30 en La Paz: el código debe ser del día de La Paz
    final Clock clock = Clock.fixed(Instant.parse("2026-11-11T01:30:00Z"), ZoneId.of("UTC"));

    OrderServiceImpl service;
    CashSession session;
    TableMarker marker;
    PricedOrder priced;

    @BeforeEach
    void setUp() {
        service = new OrderServiceImpl(orderRepository, historyRepository, new OrderMapper(), new PagePojoMapper(), pricingService,
                cashSessionService, paymentService, tableMarkerService, parameterService, jwtService, eventPublisher, clock);
        session = new CashSession();
        session.setId(3);
        marker = new TableMarker();
        marker.setId(7);
        marker.setNumber(5);
        marker.setActive(true);

        Category category = new Category();
        category.setId(1);
        Product product = new Product();
        product.setId(10);
        product.setName("Capuchino");
        product.setCategory(category);
        Addon addon = new Addon();
        addon.setId(5);
        addon.setName("Leche de almendra");
        addon.setPrice(new BigDecimal("3.50"));
        priced = new PricedOrder(
                List.of(new PricedOrder.Line(product, "Bebidas", 1, 2, new BigDecimal("15.00"), List.of(addon),
                        new BigDecimal("3.50"), new BigDecimal("37.00"), "sin azúcar")),
                new BigDecimal("37.00"), DiscountTypeEnum.FIXED_AMOUNT, new BigDecimal("2.00"),
                new BigDecimal("2.00"), new BigDecimal("5.41"), "Cliente frecuente", new BigDecimal("35.00"));

        when(cashSessionService.getOpenSessionOfCurrentUser()).thenReturn(session);
        when(pricingService.price(any())).thenReturn(priced);
        when(tableMarkerService.getById(7)).thenReturn(marker);
        when(jwtService.getCurrentUsername()).thenReturn("maria.caja");
        PaymentMethod cash = new PaymentMethod();
        when(paymentService.resolve(anyList(), any()))
                .thenReturn(List.of(new ResolvedPayment(cash, new BigDecimal("35.00"), null)));
        when(orderRepository.nextDailyCode(any())).thenReturn(3);
        when(orderRepository.saveAndFlush(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(40);
            return o;
        });
        when(paymentService.register(anyInt(), anyString(), any(), anyList())).thenReturn(List.of());
    }

    private OrderDto dineIn() {
        OrderDto dto = new OrderDto();
        dto.setServiceType("DINE_IN");
        dto.setTableMarkerId(7);
        PaymentDto payment = new PaymentDto();
        payment.setPaymentMethodId(1);
        payment.setAmount(new BigDecimal("35.00"));
        dto.setPayments(List.of(payment));
        return dto;
    }

    @Test
    void create_savesSnapshotsHistoryAndPayments_inOneFlow() {
        OrderPojo result = service.create(dineIn());

        ArgumentCaptor<Order> saved = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).saveAndFlush(saved.capture());
        Order order = saved.getValue();
        assertThat(order.getStatus()).isEqualTo(OrderStatusEnum.PENDING);
        assertThat(order.getBusinessDate()).isEqualTo(LocalDate.of(2026, 11, 10));
        assertThat(order.getCode()).isEqualTo(3);
        assertThat(order.getTableMarkerNumber()).isEqualTo(5);
        assertThat(order.getTotal()).isEqualByComparingTo("35.00");
        assertThat(order.getDiscountAppliedBy()).isEqualTo("maria.caja");
        assertThat(order.getCashSession()).isSameAs(session);
        assertThat(order.getItems()).hasSize(1);
        assertThat(order.getItems().getFirst().getProductName()).isEqualTo("Capuchino");
        assertThat(order.getItems().getFirst().getAddons().getFirst().getUnitPrice()).isEqualByComparingTo("3.50");

        ArgumentCaptor<OrderStatusHistory> history = ArgumentCaptor.forClass(OrderStatusHistory.class);
        verify(historyRepository).save(history.capture());
        assertThat(history.getValue().getFromStatus()).isNull();
        assertThat(history.getValue().getToStatus()).isEqualTo(OrderStatusEnum.PENDING);

        verify(paymentService).register(eq(40), eq("#003"), same(session), anyList());
        assertThat(result.getCodeLabel()).isEqualTo("#003");
        assertThat(result.getServiceType()).isEqualTo(ServiceTypeEnum.DINE_IN);
    }

    @Test
    void create_fails_whenNoOpenCashSession() {
        when(cashSessionService.getOpenSessionOfCurrentUser())
                .thenThrow(new ValidationErrorException("Debe abrir caja antes de registrar cobros o movimientos."));

        assertThatThrownBy(() -> service.create(dineIn())).isInstanceOf(ValidationErrorException.class);
        verify(orderRepository, never()).saveAndFlush(any());
    }

    @Test
    void create_returns409_whenMarkerAlreadyInUse() {
        when(orderRepository.existsByTableMarkerIdAndStatusIn(eq(7), any())).thenReturn(true);

        assertThatThrownBy(() -> service.create(dineIn()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("número de mesa 5");
        verify(orderRepository, never()).saveAndFlush(any());
    }

    @Test
    void create_translatesUniqueIndexRace_toConflict() {
        when(orderRepository.saveAndFlush(any(Order.class))).thenThrow(new DataIntegrityViolationException("dup",
                new SQLException("duplicate key value violates unique constraint \"ux_orders_marker_open\"")));

        assertThatThrownBy(() -> service.create(dineIn())).isInstanceOf(ConflictException.class);
        verify(paymentService, never()).register(anyInt(), anyString(), any(), anyList());
    }

    @Test
    void create_requiresMarker_forDineIn() {
        OrderDto dto = dineIn();
        dto.setTableMarkerId(null);
        assertThatThrownBy(() -> service.create(dto)).isInstanceOf(ValidationErrorException.class);
    }

    @Test
    void create_rejectsInactiveMarker() {
        marker.setActive(false);
        assertThatThrownBy(() -> service.create(dineIn())).isInstanceOf(ValidationErrorException.class);
    }

    @Test
    void create_rejectsMarker_forTakeaway() {
        OrderDto dto = dineIn();
        dto.setServiceType("TAKEAWAY");
        assertThatThrownBy(() -> service.create(dto)).isInstanceOf(ValidationErrorException.class);
    }

    @Test
    void create_takeaway_hasNoMarker_andKeepsCustomerName() {
        OrderDto dto = dineIn();
        dto.setServiceType("TAKEAWAY");
        dto.setTableMarkerId(null);
        dto.setCustomerName("  Ana ");

        service.create(dto);

        ArgumentCaptor<Order> saved = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getTableMarker()).isNull();
        assertThat(saved.getValue().getCustomerName()).isEqualTo("Ana");
    }

    @Test
    void create_doesNotSave_whenPaymentsInvalid() {
        when(paymentService.resolve(anyList(), any())).thenThrow(new ValidationErrorException("La suma no coincide"));
        assertThatThrownBy(() -> service.create(dineIn())).isInstanceOf(ValidationErrorException.class);
        verify(orderRepository, never()).saveAndFlush(any());
        verify(orderRepository, never()).nextDailyCode(any());
    }
}
