package bo.cdr.app.appcash.domain.service.implement;

import bo.cdr.app.appbase.domain.mapper.PagePojoMapper;
import bo.cdr.app.appcash.data.CashMovementRepository;
import bo.cdr.app.appcash.data.CashSessionRepository;
import bo.cdr.app.appcash.data.CashSessionSummaryRepository;
import bo.cdr.app.appcash.domain.entity.CashSession;
import bo.cdr.app.appcash.domain.entity.CashSessionSummary;
import bo.cdr.app.appcash.domain.mapper.CashSessionMapper;
import bo.cdr.app.appcash.presentation.request.dto.CashCloseDto;
import bo.cdr.app.appcash.presentation.request.dto.CashDeclaredDto;
import bo.cdr.app.appcash.presentation.request.filter.CashSessionFilter;
import bo.cdr.app.appcash.presentation.response.pojo.CashClosePreviewPojo;
import bo.cdr.app.appcash.presentation.response.pojo.CashMovementPojo;
import bo.cdr.app.appcash.presentation.response.pojo.CashOpenOrderPojo;
import bo.cdr.app.appcatalog.domain.entity.PaymentMethod;
import bo.cdr.app.appcatalog.domain.service.interfaces.PaymentMethodService;
import bo.cdr.app.appsecurity.auth.jwt.JwtService;
import bo.cdr.app.appsecurity.domain.entity.UserApp;
import bo.cdr.app.appsecurity.domain.service.interfaces.UserService;
import bo.cdr.app.common.enums.CashMovementTypeEnum;
import bo.cdr.app.common.enums.CashSessionStatusEnum;
import bo.cdr.app.common.enums.OrderStatusEnum;
import bo.cdr.app.exception.ConflictException;
import bo.cdr.app.exception.NotAllowedException;
import bo.cdr.app.exception.response.ValidationErrorException;
import bo.cdr.app.util.constants.RoleCodes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Etapa 5: esperado por medio, arqueo, cierre (dueño / ADMIN con nota), historial por rol. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CashCloseTest {

    @Mock CashSessionRepository cashSessionRepository;
    @Mock CashMovementRepository cashMovementRepository;
    @Mock CashSessionSummaryRepository summaryRepository;
    @Mock PaymentMethodService paymentMethodService;
    @Mock UserService userService;
    @Mock JwtService jwtService;

    final Clock clock = Clock.fixed(Instant.parse("2026-11-11T22:00:00Z"), ZoneId.of("UTC"));
    final LocalDateTime t = LocalDateTime.of(2026, 11, 11, 10, 0);
    CashSessionServiceImpl service;
    CashSession session;
    PaymentMethod cash;
    PaymentMethod qr;

    @BeforeEach
    void setUp() {
        service = new CashSessionServiceImpl(cashSessionRepository, cashMovementRepository, summaryRepository,
                new CashSessionMapper(), new PagePojoMapper(), paymentMethodService, userService, jwtService, clock);
        UserApp maria = new UserApp();
        maria.setId(4);
        maria.setUsername("maria.caja");
        maria.setFullName("María Caja");
        session = new CashSession();
        session.setId(3);
        session.setUser(maria);
        session.setStatus(CashSessionStatusEnum.OPEN);
        session.setOpeningAmount(new BigDecimal("100.00"));
        session.setVersion(7L);
        cash = method(1, "EFECTIVO", "Efectivo");
        qr = method(2, "QR", "QR");

        when(cashSessionRepository.findById(3)).thenReturn(Optional.of(session));
        when(cashSessionRepository.findByIdForUpdate(3)).thenReturn(Optional.of(session));
        when(paymentMethodService.getCashMethod()).thenReturn(cash);
        when(paymentMethodService.getActiveIncluding(any())).thenReturn(List.of(cash, qr));
        when(cashMovementRepository.findPojoByCashSessionId(3)).thenReturn(List.of(
                movement(CashMovementTypeEnum.OPENING, null, "100.00"),
                movement(CashMovementTypeEnum.SALE, cash, "35.00"),
                movement(CashMovementTypeEnum.SALE, qr, "20.00"),
                movement(CashMovementTypeEnum.REFUND, cash, "15.00"),
                movement(CashMovementTypeEnum.INCOME, null, "10.00"),
                movement(CashMovementTypeEnum.EXPENSE, null, "4.50")));
        when(cashSessionRepository.findOrdersInStatuses(eq3(), any())).thenReturn(List.of());
        asUser(4, RoleCodes.CAJERO);
    }

    private static Integer eq3() {
        return org.mockito.ArgumentMatchers.eq(3);
    }

    private static PaymentMethod method(int id, String code, String name) {
        PaymentMethod m = new PaymentMethod();
        m.setId(id);
        m.setCode(code);
        m.setName(name);
        return m;
    }

    private CashMovementPojo movement(CashMovementTypeEnum type, PaymentMethod method, String amount) {
        return new CashMovementPojo(1, type, method == null ? null : method.getId(), method == null ? null : method.getCode(),
                method == null ? null : method.getName(), new BigDecimal(amount), null, null, "x", t);
    }

    private void asUser(int id, String role) {
        when(jwtService.getCurrentUserId()).thenReturn(id);
        when(jwtService.getCurrentUsername()).thenReturn(id == 4 ? "maria.caja" : "admin");
        when(jwtService.getCurrentAuthorities()).thenReturn(List.of(role));
    }

    private static CashCloseDto close(long version, String note, String cashAmount, String qrAmount) {
        CashCloseDto dto = new CashCloseDto();
        dto.setExpectedVersion(version);
        dto.setNote(note);
        dto.setDeclared(List.of(declared(1, cashAmount), declared(2, qrAmount)));
        return dto;
    }

    private static CashDeclaredDto declared(int methodId, String amount) {
        CashDeclaredDto dto = new CashDeclaredDto();
        dto.setPaymentMethodId(methodId);
        dto.setAmount(new BigDecimal(amount));
        return dto;
    }

    @Test
    void expectedByMethod_followsDomainFormula() {
        Map<Integer, BigDecimal> expected = new CashSessionMapper().expectedByMethod(new BigDecimal("100.00"),
                cashMovementRepository.findPojoByCashSessionId(3), 1);
        // efectivo: 100 + 35 − 15 + 10 − 4.50 = 125.50 ; QR: 20
        assertThat(expected.get(1)).isEqualByComparingTo("125.50");
        assertThat(expected.get(2)).isEqualByComparingTo("20.00");
    }

    @Test
    void preview_listsCashFirst_andWarnsAboutUndeliveredOrders() {
        when(cashSessionRepository.findOrdersInStatuses(eq3(), any()))
                .thenReturn(List.of(new CashOpenOrderPojo(40, 3, OrderStatusEnum.READY)));

        CashClosePreviewPojo preview = service.getClosePreview(3);

        assertThat(preview.methods()).extracting(m -> m.paymentMethodCode()).containsExactly("EFECTIVO", "QR");
        assertThat(preview.methods().getFirst().cash()).isTrue();
        assertThat(preview.methods().getFirst().expectedAmount()).isEqualByComparingTo("125.50");
        assertThat(preview.openOrders()).extracting(CashOpenOrderPojo::codeLabel).containsExactly("#003");
        assertThat(preview.ownSession()).isTrue();
        assertThat(preview.version()).isEqualTo(7L);
    }

    @Test
    void close_savesSummaryPerMethod_withDifference_andClosesSession() {
        service.close(3, close(7, null, "120.00", "20.00"));

        ArgumentCaptor<CashSessionSummary> summaries = ArgumentCaptor.forClass(CashSessionSummary.class);
        verify(summaryRepository, times(2)).save(summaries.capture());
        CashSessionSummary cashSummary = summaries.getAllValues().getFirst();
        assertThat(cashSummary.getExpectedAmount()).isEqualByComparingTo("125.50");
        assertThat(cashSummary.getDeclaredAmount()).isEqualByComparingTo("120.00");
        assertThat(cashSummary.getDifference()).isEqualByComparingTo("-5.50");   // faltante
        assertThat(summaries.getAllValues().get(1).getDifference()).isEqualByComparingTo("0.00");

        assertThat(session.getStatus()).isEqualTo(CashSessionStatusEnum.CLOSED);
        assertThat(session.getClosedBy()).isEqualTo("maria.caja");
        assertThat(session.getClosedAt()).isNotNull();
        verify(cashSessionRepository).saveAndFlush(session);
    }

    @Test
    void close_returns409_whenSessionChangedSincePreview() {
        assertThatThrownBy(() -> service.close(3, close(6, null, "125.50", "20.00")))
                .isInstanceOf(ConflictException.class);
        verifyNoInteractions(summaryRepository);
    }

    @Test
    void close_requiresOneAmountPerMethod() {
        CashCloseDto dto = close(7, null, "125.50", "20.00");
        dto.setDeclared(List.of(declared(1, "125.50")));
        assertThatThrownBy(() -> service.close(3, dto)).isInstanceOf(ValidationErrorException.class);

        dto.setDeclared(List.of(declared(1, "1"), declared(1, "2")));
        assertThatThrownBy(() -> service.close(3, dto)).isInstanceOf(ValidationErrorException.class);
    }

    @Test
    void close_rejectsAlreadyClosedSession() {
        session.setStatus(CashSessionStatusEnum.CLOSED);
        assertThatThrownBy(() -> service.close(3, close(7, null, "0", "0")))
                .isInstanceOf(ValidationErrorException.class)
                .hasMessageContaining("cerrada");
    }

    @Test
    void close_cashierCannotCloseOthersSession() {
        asUser(9, RoleCodes.CAJERO);
        assertThatThrownBy(() -> service.close(3, close(7, "x", "0", "0"))).isInstanceOf(NotAllowedException.class);
    }

    @Test
    void close_adminClosingOthersSession_requiresNote() {
        asUser(1, RoleCodes.ADMIN);
        assertThatThrownBy(() -> service.close(3, close(7, "  ", "125.50", "20.00")))
                .isInstanceOf(ValidationErrorException.class)
                .hasMessageContaining("María Caja");

        service.close(3, close(7, "Olvidó cerrar al terminar el turno", "125.50", "20.00"));
        assertThat(session.getClosedBy()).isEqualTo("admin");
        assertThat(session.getClosingNote()).isEqualTo("Olvidó cerrar al terminar el turno");
    }

    @Test
    void getById_cashierCannotSeeOthersSession() {
        asUser(9, RoleCodes.CAJERO);
        assertThatThrownBy(() -> service.getCashSessionPojoById(3)).isInstanceOf(NotAllowedException.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    void pageable_forcesCashierToOwnSessions() {
        when(cashSessionRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn((Page<CashSession>) new PageImpl<>(List.of(session)));
        CashSessionFilter filter = new CashSessionFilter();
        filter.setUserId(99);

        service.pageable(0, 10, null, null, filter);

        assertThat(filter.getUserId()).isEqualTo(4);
    }

    @Test
    void openSessions_onlyForAdmin() {
        assertThatThrownBy(() -> service.getOpenSessions()).isInstanceOf(NotAllowedException.class);
        asUser(1, RoleCodes.ADMIN);
        when(cashSessionRepository.findAllOpen()).thenReturn(List.of(session));
        assertThat(service.getOpenSessions()).hasSize(1);
    }
}
