package bo.cdr.app.apporder.domain.service.implement;

import bo.cdr.app.appbase.data.repository.GenericRepository;
import bo.cdr.app.appbase.domain.mapper.PagePojoMapper;
import bo.cdr.app.appbase.domain.service.CRUDServiceImpl;
import bo.cdr.app.appbase.presentation.response.PagePojo;
import bo.cdr.app.appcash.domain.entity.CashSession;
import bo.cdr.app.appcash.domain.model.ResolvedPayment;
import bo.cdr.app.appcash.domain.service.interfaces.CashSessionService;
import bo.cdr.app.appcash.domain.service.interfaces.PaymentService;
import bo.cdr.app.appcash.presentation.response.pojo.PaymentPojo;
import bo.cdr.app.appcatalog.domain.entity.TableMarker;
import bo.cdr.app.appcatalog.domain.service.interfaces.ParameterService;
import bo.cdr.app.appcatalog.domain.service.interfaces.TableMarkerService;
import bo.cdr.app.apporder.data.OrderRepository;
import bo.cdr.app.apporder.data.OrderStatusHistoryRepository;
import bo.cdr.app.apporder.domain.entity.Order;
import bo.cdr.app.apporder.domain.entity.OrderStatusHistory;
import bo.cdr.app.apporder.domain.event.OrderChangedEvent;
import bo.cdr.app.apporder.domain.mapper.OrderMapper;
import bo.cdr.app.apporder.domain.model.OrderStateMachine;
import bo.cdr.app.apporder.domain.model.PricedOrder;
import bo.cdr.app.apporder.domain.service.interfaces.OrderPricingService;
import bo.cdr.app.apporder.domain.service.interfaces.OrderService;
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
import bo.cdr.app.appsecurity.auth.jwt.JwtService;
import bo.cdr.app.common.enums.OrderEventTypeEnum;
import bo.cdr.app.common.enums.OrderStatusEnum;
import bo.cdr.app.common.enums.ServiceTypeEnum;
import bo.cdr.app.exception.ConflictException;
import bo.cdr.app.exception.NotAllowedException;
import bo.cdr.app.exception.response.ValidationErrorException;
import bo.cdr.app.util.SortUtil;
import bo.cdr.app.util.constants.BusinessZone;
import bo.cdr.app.util.constants.ParameterCodes;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Pedidos (skill cdr-order-management §4–§9). */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl extends CRUDServiceImpl<Order, Integer> implements OrderService {

    private static final String MARKER_OPEN_INDEX = "ux_orders_marker_open";
    private static final Set<String> SORTABLE = Set.of("id", "createdAt", "businessDate", "code", "total", "status");
    private static final int MAX_PAGE_SIZE = 100;

    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final OrderMapper orderMapper;
    private final PagePojoMapper pagePojoMapper;
    private final OrderPricingService orderPricingService;
    private final CashSessionService cashSessionService;
    private final PaymentService paymentService;
    private final TableMarkerService tableMarkerService;
    private final ParameterService parameterService;
    private final JwtService jwtService;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @Override
    protected GenericRepository<Order, Integer> getRepository() {
        return orderRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public OrderQuotePojo quote(OrderQuoteDto dto) {
        return orderMapper.toQuotePojo(orderPricingService.price(dto));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderPojo create(OrderDto dto) {
        CashSession cashSession = cashSessionService.getOpenSessionOfCurrentUser();
        PricedOrder priced = orderPricingService.price(dto);
        ServiceTypeEnum serviceType = ServiceTypeEnum.fromString(dto.getServiceType());
        TableMarker marker = resolveMarker(serviceType, dto.getTableMarkerId());
        List<ResolvedPayment> payments = paymentService.resolve(dto.getPayments(), priced.total());

        String username = jwtService.getCurrentUsername();
        LocalDateTime now = LocalDateTime.now(clock);
        String customerName = dto.getCustomerName() == null || dto.getCustomerName().isBlank()
                ? null : dto.getCustomerName().trim();

        Order order = orderMapper.fromPriced(priced, serviceType, marker, customerName, cashSession, username, now);
        LocalDate businessDate = LocalDate.now(clock.withZone(BusinessZone.ZONE));
        order.setBusinessDate(businessDate);
        order.setCode(orderRepository.nextDailyCode(businessDate));
        saveAndFlushHandlingMarkerRace(order);

        recordHistory(order.getId(), null, OrderStatusEnum.PENDING, username, now, null);

        String label = OrderMapper.codeLabel(order.getCode());
        List<PaymentPojo> paymentPojos = paymentService.register(order.getId(), label, cashSession, payments);

        log.info("Pedido {} (id {}) registrado por {}: {} {}, total Bs {}{}", label, order.getId(), username,
                serviceType, marker == null ? "" : "mesa " + marker.getNumber(), order.getTotal(),
                priced.hasDiscount() ? ", descuento Bs " + priced.discountAmount() : "");
        eventPublisher.publishEvent(new OrderChangedEvent(order.getId(), OrderEventTypeEnum.ORDER_CREATED));
        OrderPojo pojo = orderMapper.toPojo(order, paymentPojos);
        pojo.setStatusHistory(historyOf(order.getId()));
        return pojo;
    }

    @Override
    @Transactional(readOnly = true)
    public OrderPojo getOrderPojoById(Integer id) {
        Order order = getById(id);
        OrderPojo pojo = orderMapper.toPojo(order, paymentService.getByOrderId(id));
        pojo.setStatusHistory(historyOf(id));
        return pojo;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderBoardPojo> getBoard(List<OrderStatusEnum> statuses) {
        List<OrderStatusEnum> filter = statuses == null || statuses.isEmpty()
                ? List.copyOf(OrderStatusEnum.OPEN_STATUSES) : statuses;
        return orderRepository.findByStatusInOrderByCreatedAtAscIdAsc(filter).stream()
                .map(orderMapper::toBoardPojo)
                .toList();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public OrderBoardPojo getBoardPojo(Integer id) {
        return orderMapper.toBoardPojo(getById(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderPojo changeStatus(Integer id, OrderStatusDto dto) {
        Order order = getById(id);
        checkVersion(order, dto.getExpectedVersion());
        OrderStatusEnum from = order.getStatus();
        OrderStatusEnum to = OrderStatusEnum.fromString(dto.getToStatus());
        String label = OrderMapper.codeLabel(order.getCode());

        if (to == OrderStatusEnum.CANCELLED) {
            throw new ValidationErrorException("Para cancelar un pedido use la opción Cancelar (exige motivo).");
        }
        if (!OrderStateMachine.exists(from, to)) {
            throw new ValidationErrorException(String.format("El pedido %s no puede pasar de %s a %s.",
                    label, from.getLabel(), to.getLabel()));
        }
        if (!OrderStateMachine.isAllowed(from, to, jwtService.getCurrentAuthorities())) {
            throw new NotAllowedException(String.format("Su rol no permite pasar un pedido de %s a %s.",
                    from.getLabel(), to.getLabel()));
        }

        order.setStatus(to);
        orderRepository.saveAndFlush(order);   // @Version: dos usuarios a la vez → uno recibe 409
        String username = jwtService.getCurrentUsername();
        String note = dto.getNote() == null || dto.getNote().isBlank() ? null : dto.getNote().trim();
        recordHistory(order.getId(), from, to, username, LocalDateTime.now(clock), note);

        log.info("Pedido {} (id {}): {} → {} por {}", label, id, from, to, username);
        eventPublisher.publishEvent(new OrderChangedEvent(id, OrderEventTypeEnum.ORDER_STATUS_CHANGED));
        return getOrderPojoById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderPojo cancel(Integer id, OrderCancelDto dto) {
        Order order = getById(id);
        checkVersion(order, dto.getExpectedVersion());
        OrderStatusEnum from = order.getStatus();
        String label = OrderMapper.codeLabel(order.getCode());

        if (!OrderStatusEnum.OPEN_STATUSES.contains(from)) {
            throw new ValidationErrorException("El pedido " + label + " está " + from.getLabel() + " y no se puede cancelar.");
        }
        if (!OrderStateMachine.canCancel(from, jwtService.getCurrentAuthorities())) {
            throw new NotAllowedException("Solo un administrador puede cancelar un pedido " + from.getLabel() + ".");
        }
        // La devolución sale de la caja de quien cancela (debe tenerla abierta)
        CashSession cashSession = cashSessionService.getOpenSessionOfCurrentUser();

        String username = jwtService.getCurrentUsername();
        LocalDateTime now = LocalDateTime.now(clock);
        String reason = dto.getReason().trim();
        order.setStatus(OrderStatusEnum.CANCELLED);
        order.setCancelReason(reason);
        order.setCancelledBy(username);
        order.setCancelledAt(now);
        orderRepository.saveAndFlush(order);

        recordHistory(id, from, OrderStatusEnum.CANCELLED, username, now, reason);
        paymentService.refundOrderPayments(id, label, cashSession);

        log.info("Pedido {} (id {}) cancelado por {} desde {}: {}", label, id, username, from, reason);
        eventPublisher.publishEvent(new OrderChangedEvent(id, OrderEventTypeEnum.ORDER_CANCELLED));
        return getOrderPojoById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public PagePojo<OrderListPojo> pageable(int page, int size, String sortBy, String sortOrder, OrderFilter filter) {
        PageRequest pageRequest = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                SortUtil.build(sortBy, sortOrder, SORTABLE));
        Page<OrderListPojo> result = orderRepository.findAll(toSpecification(filter), pageRequest)
                .map(orderMapper::toListPojo);
        return pagePojoMapper.convertToPagePojo(result);
    }

    @Override
    @Transactional(readOnly = true)
    public KitchenSettingsPojo getKitchenSettings() {
        return new KitchenSettingsPojo(
                parameterService.getDecimalValue(ParameterCodes.KITCHEN_DELAY_WARNING_MINUTES).intValue(),
                parameterService.getDecimalValue(ParameterCodes.KITCHEN_DELAY_CRITICAL_MINUTES).intValue());
    }

    private Specification<Order> toSpecification(OrderFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filter != null) {
                if (filter.getDateFrom() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("businessDate"), filter.getDateFrom()));
                }
                if (filter.getDateTo() != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("businessDate"), filter.getDateTo()));
                }
                if (filter.getStatus() != null && !filter.getStatus().isBlank()) {
                    predicates.add(cb.equal(root.get("status"), OrderStatusEnum.fromString(filter.getStatus())));
                }
                if (filter.getServiceType() != null && !filter.getServiceType().isBlank()) {
                    predicates.add(cb.equal(root.get("serviceType"), ServiceTypeEnum.fromString(filter.getServiceType())));
                }
                if (filter.getCode() != null) {
                    predicates.add(cb.equal(root.get("code"), filter.getCode()));
                }
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private void checkVersion(Order order, Long expectedVersion) {
        if (expectedVersion == null || !expectedVersion.equals(order.getVersion())) {
            throw new ConflictException("El pedido " + OrderMapper.codeLabel(order.getCode())
                    + " fue modificado por otro usuario. Se actualizó su estado; revise e intente de nuevo.");
        }
    }

    private void recordHistory(Integer orderId, OrderStatusEnum from, OrderStatusEnum to, String username,
                               LocalDateTime when, String note) {
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrderId(orderId);
        history.setFromStatus(from);
        history.setToStatus(to);
        history.setChangedBy(username);
        history.setChangedAt(when);
        history.setNote(note);
        orderStatusHistoryRepository.save(history);
    }

    private List<bo.cdr.app.apporder.presentation.response.pojo.OrderStatusHistoryPojo> historyOf(Integer orderId) {
        return orderStatusHistoryRepository.findByOrderIdOrderByIdAsc(orderId).stream()
                .map(orderMapper::toHistoryPojo)
                .toList();
    }

    private TableMarker resolveMarker(ServiceTypeEnum serviceType, Integer tableMarkerId) {
        if (serviceType == ServiceTypeEnum.TAKEAWAY) {
            if (tableMarkerId != null) {
                throw new ValidationErrorException("Un pedido para llevar no lleva número de mesa.");
            }
            return null;
        }
        if (tableMarkerId == null) {
            throw new ValidationErrorException("Debe elegir el número de mesa que entrega al cliente.");
        }
        TableMarker marker = tableMarkerService.getById(tableMarkerId);
        if (!Boolean.TRUE.equals(marker.getActive())) {
            throw new ValidationErrorException("El número de mesa " + marker.getNumber() + " está inactivo.");
        }
        if (orderRepository.existsByTableMarkerIdAndStatusIn(marker.getId(), OrderStatusEnum.OPEN_STATUSES)) {
            throw markerInUse(marker);
        }
        return marker;
    }

    /** El índice ux_orders_marker_open resuelve a dos cajeros que eligen el mismo adorno a la vez. */
    private void saveAndFlushHandlingMarkerRace(Order order) {
        try {
            orderRepository.saveAndFlush(order);
        } catch (DataIntegrityViolationException e) {
            if (order.getTableMarker() != null && e.getMostSpecificCause().getMessage() != null
                    && e.getMostSpecificCause().getMessage().contains(MARKER_OPEN_INDEX)) {
                throw markerInUse(order.getTableMarker());
            }
            throw e;
        }
    }

    private ConflictException markerInUse(TableMarker marker) {
        return new ConflictException("El número de mesa " + marker.getNumber() + " está en uso. Elija otro.");
    }
}
