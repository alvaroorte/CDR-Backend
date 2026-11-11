package bo.cdr.app.appcash.domain.service.implement;

import bo.cdr.app.appbase.data.repository.GenericRepository;
import bo.cdr.app.appbase.domain.mapper.PagePojoMapper;
import bo.cdr.app.appbase.domain.service.CRUDServiceImpl;
import bo.cdr.app.appbase.presentation.response.PagePojo;
import bo.cdr.app.appcash.data.CashMovementRepository;
import bo.cdr.app.appcash.data.CashSessionRepository;
import bo.cdr.app.appcash.data.CashSessionSummaryRepository;
import bo.cdr.app.appcash.domain.entity.CashMovement;
import bo.cdr.app.appcash.domain.entity.CashSession;
import bo.cdr.app.appcash.domain.entity.CashSessionSummary;
import bo.cdr.app.appcash.domain.mapper.CashSessionMapper;
import bo.cdr.app.appcash.domain.service.interfaces.CashSessionService;
import bo.cdr.app.appcash.presentation.request.dto.CashCloseDto;
import bo.cdr.app.appcash.presentation.request.dto.CashDeclaredDto;
import bo.cdr.app.appcash.presentation.request.dto.CashMovementDto;
import bo.cdr.app.appcash.presentation.request.dto.CashSessionOpenDto;
import bo.cdr.app.appcash.presentation.request.filter.CashSessionFilter;
import bo.cdr.app.appcash.presentation.response.pojo.CashClosePreviewPojo;
import bo.cdr.app.appcash.presentation.response.pojo.CashMethodExpectedPojo;
import bo.cdr.app.appcash.presentation.response.pojo.CashMovementPojo;
import bo.cdr.app.appcash.presentation.response.pojo.CashSessionListPojo;
import bo.cdr.app.appcash.presentation.response.pojo.CashSessionPojo;
import bo.cdr.app.appcash.presentation.response.pojo.CashSummaryPojo;
import bo.cdr.app.appcatalog.domain.entity.PaymentMethod;
import bo.cdr.app.appcatalog.domain.service.interfaces.PaymentMethodService;
import bo.cdr.app.appsecurity.auth.jwt.JwtService;
import bo.cdr.app.appsecurity.domain.service.interfaces.UserService;
import bo.cdr.app.common.MoneyUtil;
import bo.cdr.app.common.enums.CashMovementTypeEnum;
import bo.cdr.app.common.enums.CashSessionStatusEnum;
import bo.cdr.app.common.enums.OrderStatusEnum;
import bo.cdr.app.exception.ConflictException;
import bo.cdr.app.exception.NotAllowedException;
import bo.cdr.app.exception.response.ValidationErrorException;
import bo.cdr.app.util.SortUtil;
import bo.cdr.app.util.constants.RoleCodes;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Caja por usuario (skill cdr-order-management §10): apertura, movimientos, arqueo y cierre, historial.
 * Decisiones de la Etapa 5: se puede cerrar con pedidos sin entregar (advertencia en el preview);
 * se declaran todos los medios; un ADMIN puede cerrar la caja de otro con nota obligatoria;
 * el ADMIN ve el historial de todos y el CAJERO solo el suyo.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CashSessionServiceImpl extends CRUDServiceImpl<CashSession, Integer> implements CashSessionService {

    private static final Set<String> SORTABLE = Set.of("id", "openedAt", "closedAt", "status");
    private static final int MAX_PAGE_SIZE = 100;

    private final CashSessionRepository cashSessionRepository;
    private final CashMovementRepository cashMovementRepository;
    private final CashSessionSummaryRepository cashSessionSummaryRepository;
    private final CashSessionMapper cashSessionMapper;
    private final PagePojoMapper pagePojoMapper;
    private final PaymentMethodService paymentMethodService;
    private final UserService userService;
    private final JwtService jwtService;
    private final Clock clock;

    @Override
    protected GenericRepository<CashSession, Integer> getRepository() {
        return cashSessionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CashSessionPojo> getCurrent() {
        return cashSessionRepository.findOpenByUserId(jwtService.getCurrentUserId()).map(this::toPojo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CashSessionPojo open(CashSessionOpenDto dto) {
        Integer userId = jwtService.getCurrentUserId();
        if (cashSessionRepository.findOpenByUserId(userId).isPresent()) {
            throw new ConflictException("Ya tiene una caja abierta.");
        }
        CashSession session = new CashSession();
        session.setUser(userService.getById(userId));
        session.setStatus(CashSessionStatusEnum.OPEN);
        session.setOpeningAmount(MoneyUtil.round(dto.getOpeningAmount()));
        session.setOpenedAt(LocalDateTime.now(clock));
        // saveAndFlush: el índice ux_cash_sessions_user_open resuelve dos aperturas simultáneas (409)
        cashSessionRepository.saveAndFlush(session);

        CashMovement opening = new CashMovement();
        opening.setCashSession(session);
        opening.setType(CashMovementTypeEnum.OPENING);
        opening.setAmount(session.getOpeningAmount());
        opening.setDescription("Apertura de caja");
        cashMovementRepository.save(opening);

        log.info("Caja {} abierta por {} con Bs {}", session.getId(), jwtService.getCurrentUsername(),
                session.getOpeningAmount());
        return toPojo(session);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CashSessionPojo registerMovement(CashMovementDto dto) {
        CashMovementTypeEnum type = CashMovementTypeEnum.fromString(dto.getType());
        if (type != CashMovementTypeEnum.INCOME && type != CashMovementTypeEnum.EXPENSE) {
            throw new ValidationErrorException("Solo se pueden registrar ingresos o egresos manuales.");
        }
        CashSession session = getOpenSessionOfCurrentUser();

        CashMovement movement = new CashMovement();
        movement.setCashSession(session);
        movement.setType(type);
        movement.setAmount(MoneyUtil.round(dto.getAmount()));
        movement.setDescription(dto.getDescription().trim());
        cashMovementRepository.save(movement);

        log.info("Caja {}: {} de Bs {} ({})", session.getId(), type, movement.getAmount(), movement.getDescription());
        return toPojo(session);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CashSession getOpenSessionOfCurrentUser() {
        return cashSessionRepository.findOpenByUserIdForUpdate(jwtService.getCurrentUserId())
                .orElseThrow(() -> new ValidationErrorException("Debe abrir caja antes de registrar cobros o movimientos."));
    }

    @Override
    @Transactional(readOnly = true)
    public CashClosePreviewPojo getClosePreview(Integer id) {
        CashSession session = getAccessible(id);
        requireOpen(session);
        return new CashClosePreviewPojo(session.getId(), session.getVersion(), session.getUser().getId(),
                session.getUser().getFullName(), isOwn(session), session.getOpenedAt(), session.getOpeningAmount(),
                expectedMethods(session),
                cashSessionRepository.findOrdersInStatuses(session.getId(), OrderStatusEnum.OPEN_STATUSES));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CashSessionPojo close(Integer id, CashCloseDto dto) {
        CashSession session = cashSessionRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new bo.cdr.app.exception.EntityNotFoundException("Caja", id));
        checkAccess(session);
        requireOpen(session);
        if (!dto.getExpectedVersion().equals(session.getVersion())) {
            throw new ConflictException("La caja tuvo movimientos o cambios mientras hacía el arqueo. Revise los importes esperados e intente de nuevo.");
        }
        String note = dto.getNote() == null || dto.getNote().isBlank() ? null : dto.getNote().trim();
        if (!isOwn(session) && note == null) {
            throw new ValidationErrorException("Indique el motivo por el que cierra la caja de " + session.getUser().getFullName() + ".");
        }

        List<CashMethodExpectedPojo> expected = expectedMethods(session);
        Map<Integer, BigDecimal> declared = indexDeclared(dto.getDeclared());
        Set<Integer> expectedIds = expected.stream().map(CashMethodExpectedPojo::paymentMethodId).collect(Collectors.toSet());
        if (!declared.keySet().equals(expectedIds)) {
            throw new ValidationErrorException("Debe declarar un importe por cada medio de pago del arqueo (y solo esos).");
        }

        Map<Integer, PaymentMethod> methods = paymentMethodService.getActiveIncluding(expectedIds).stream()
                .collect(Collectors.toMap(PaymentMethod::getId, m -> m));
        String username = jwtService.getCurrentUsername();
        BigDecimal totalDifference = BigDecimal.ZERO;
        for (CashMethodExpectedPojo method : expected) {
            BigDecimal declaredAmount = MoneyUtil.round(declared.get(method.paymentMethodId()));
            CashSessionSummary summary = new CashSessionSummary();
            summary.setCashSession(session);
            summary.setPaymentMethod(methods.get(method.paymentMethodId()));
            summary.setExpectedAmount(method.expectedAmount());
            summary.setDeclaredAmount(declaredAmount);
            summary.setDifference(MoneyUtil.round(declaredAmount.subtract(method.expectedAmount())));
            cashSessionSummaryRepository.save(summary);
            totalDifference = totalDifference.add(summary.getDifference());
        }

        session.setStatus(CashSessionStatusEnum.CLOSED);
        session.setClosedAt(LocalDateTime.now(clock));
        session.setClosedBy(username);
        session.setClosingNote(note);
        cashSessionRepository.saveAndFlush(session);   // @Version: un cobro simultáneo en esta caja → uno de los dos recibe 409

        log.info("Caja {} de {} cerrada por {}; diferencia total Bs {}", id, session.getUser().getUsername(), username,
                MoneyUtil.round(totalDifference));
        return toPojo(session);
    }

    @Override
    @Transactional(readOnly = true)
    public CashSessionPojo getCashSessionPojoById(Integer id) {
        return toPojo(getAccessible(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PagePojo<CashSessionListPojo> pageable(int page, int size, String sortBy, String sortOrder,
                                                  CashSessionFilter filter) {
        CashSessionFilter effective = filter == null ? new CashSessionFilter() : filter;
        if (!isAdmin()) {
            effective.setUserId(jwtService.getCurrentUserId());   // el CAJERO solo ve sus cajas
        }
        PageRequest pageRequest = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                SortUtil.build(sortBy == null || sortBy.isBlank() ? "openedAt" : sortBy, sortOrder, SORTABLE));
        Page<CashSession> sessions = cashSessionRepository.findAll(toSpecification(effective), pageRequest);
        Map<Integer, List<CashSummaryPojo>> summaries = summariesBySession(
                sessions.getContent().stream().map(CashSession::getId).toList());
        return pagePojoMapper.convertToPagePojo(sessions.map(s ->
                cashSessionMapper.toListPojo(s, summaries.getOrDefault(s.getId(), List.of()))));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CashSessionListPojo> getOpenSessions() {
        if (!isAdmin()) {
            throw new NotAllowedException("Solo un administrador puede ver las cajas abiertas de otros usuarios.");
        }
        return cashSessionRepository.findAllOpen().stream()
                .map(s -> cashSessionMapper.toListPojo(s, List.of()))
                .toList();
    }

    // ----- apoyo -----

    /** Esperado por medio: efectivo primero, luego medios activos y los que tuvieron movimientos. */
    private List<CashMethodExpectedPojo> expectedMethods(CashSession session) {
        PaymentMethod cash = paymentMethodService.getCashMethod();
        List<CashMovementPojo> movements = cashMovementRepository.findPojoByCashSessionId(session.getId());
        Map<Integer, BigDecimal> expected = cashSessionMapper.expectedByMethod(session.getOpeningAmount(), movements, cash.getId());
        Set<Integer> ids = new HashSet<>(expected.keySet());
        ids.add(cash.getId());
        List<CashMethodExpectedPojo> result = new ArrayList<>();
        for (PaymentMethod method : paymentMethodService.getActiveIncluding(ids)) {
            boolean isCash = method.getId().equals(cash.getId());
            result.add(new CashMethodExpectedPojo(method.getId(), method.getCode(), method.getName(), isCash,
                    MoneyUtil.round(expected.getOrDefault(method.getId(), BigDecimal.ZERO))));
        }
        if (result.stream().noneMatch(CashMethodExpectedPojo::cash)) {
            result.add(new CashMethodExpectedPojo(cash.getId(), cash.getCode(), cash.getName(), true,
                    MoneyUtil.round(expected.getOrDefault(cash.getId(), BigDecimal.ZERO))));
        }
        result.sort(Comparator.comparing((CashMethodExpectedPojo m) -> !m.cash()).thenComparing(CashMethodExpectedPojo::paymentMethodName));
        return result;
    }

    private Map<Integer, BigDecimal> indexDeclared(List<CashDeclaredDto> declared) {
        Map<Integer, BigDecimal> result = new HashMap<>();
        for (CashDeclaredDto item : declared) {
            if (result.put(item.getPaymentMethodId(), item.getAmount()) != null) {
                throw new ValidationErrorException("Hay medios de pago repetidos en el arqueo.");
            }
        }
        return result;
    }

    private CashSession getAccessible(Integer id) {
        CashSession session = getById(id);
        checkAccess(session);
        return session;
    }

    private void checkAccess(CashSession session) {
        if (!isAdmin() && !isOwn(session)) {
            throw new NotAllowedException("Solo puede consultar o cerrar su propia caja.");
        }
    }

    private void requireOpen(CashSession session) {
        if (session.getStatus() != CashSessionStatusEnum.OPEN) {
            throw new ValidationErrorException("La caja ya está cerrada.");
        }
    }

    private boolean isOwn(CashSession session) {
        return session.getUser().getId().equals(jwtService.getCurrentUserId());
    }

    private boolean isAdmin() {
        return jwtService.getCurrentAuthorities().contains(RoleCodes.ADMIN);
    }

    private Map<Integer, List<CashSummaryPojo>> summariesBySession(List<Integer> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return cashSessionSummaryRepository.findPojoByCashSessionIds(ids).stream()
                .collect(Collectors.groupingBy(CashSummaryPojo::cashSessionId));
    }

    private Specification<CashSession> toSpecification(CashSessionFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filter.getUserId() != null) {
                predicates.add(cb.equal(root.get("user").get("id"), filter.getUserId()));
            }
            if (filter.getStatus() != null && !filter.getStatus().isBlank()) {
                predicates.add(cb.equal(root.get("status"), CashSessionStatusEnum.fromString(filter.getStatus())));
            }
            if (filter.getDateFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("openedAt"), filter.getDateFrom().atStartOfDay()));
            }
            if (filter.getDateTo() != null) {
                predicates.add(cb.lessThan(root.get("openedAt"), filter.getDateTo().plusDays(1).atStartOfDay()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private CashSessionPojo toPojo(CashSession session) {
        CashSessionPojo pojo = cashSessionMapper.toPojo(session, cashMovementRepository.findPojoByCashSessionId(session.getId()));
        if (session.getId() != null && session.getStatus() == CashSessionStatusEnum.CLOSED) {
            pojo.setSummaries(summariesBySession(List.of(session.getId())).getOrDefault(session.getId(), List.of()));
        }
        return pojo;
    }
}
