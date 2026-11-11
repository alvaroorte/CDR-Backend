package bo.cdr.app.appcash.domain.service.implement;

import bo.cdr.app.appcash.data.CashMovementRepository;
import bo.cdr.app.appcash.data.CashSessionRepository;
import bo.cdr.app.appcash.data.PaymentRepository;
import bo.cdr.app.appcash.domain.entity.CashMovement;
import bo.cdr.app.appcash.domain.entity.CashSession;
import bo.cdr.app.appcash.domain.entity.Payment;
import bo.cdr.app.appcash.domain.mapper.CashSessionMapper;
import bo.cdr.app.appcash.domain.model.ResolvedPayment;
import bo.cdr.app.appcash.presentation.request.dto.CashMovementDto;
import bo.cdr.app.appcash.presentation.request.dto.CashSessionOpenDto;
import bo.cdr.app.appcash.presentation.request.dto.PaymentDto;
import bo.cdr.app.appcash.presentation.response.pojo.CashMovementPojo;
import bo.cdr.app.appcash.presentation.response.pojo.CashSessionPojo;
import bo.cdr.app.appcatalog.domain.entity.PaymentMethod;
import bo.cdr.app.appcatalog.domain.service.interfaces.PaymentMethodService;
import bo.cdr.app.appsecurity.auth.jwt.JwtService;
import bo.cdr.app.appsecurity.domain.entity.UserApp;
import bo.cdr.app.appsecurity.domain.service.interfaces.UserService;
import bo.cdr.app.common.enums.CashMovementTypeEnum;
import bo.cdr.app.common.enums.CashSessionStatusEnum;
import bo.cdr.app.common.enums.PaymentStatusEnum;
import bo.cdr.app.exception.ConflictException;
import bo.cdr.app.exception.response.ValidationErrorException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CashServicesTest {

    static PaymentMethod method(int id, String code, boolean requiresReference) {
        PaymentMethod m = new PaymentMethod();
        m.setId(id);
        m.setCode(code);
        m.setName(code);
        m.setRequiresReference(requiresReference);
        m.setActive(true);
        return m;
    }

    static PaymentDto payment(int methodId, String amount, String reference) {
        PaymentDto dto = new PaymentDto();
        dto.setPaymentMethodId(methodId);
        dto.setAmount(new BigDecimal(amount));
        dto.setReference(reference);
        return dto;
    }

    @Nested
    class PaymentServiceTest {
        @Mock PaymentRepository paymentRepository;
        @Mock CashMovementRepository cashMovementRepository;
        @Mock PaymentMethodService paymentMethodService;

        PaymentServiceImpl service() {
            return new PaymentServiceImpl(paymentRepository, cashMovementRepository, paymentMethodService);
        }

        @Test
        void resolve_acceptsSplitPayment_whenSumMatchesTotal() {
            when(paymentMethodService.getById(1)).thenReturn(method(1, "EFECTIVO", false));
            when(paymentMethodService.getById(2)).thenReturn(method(2, "QR", true));

            List<ResolvedPayment> resolved = service().resolve(
                    List.of(payment(1, "20", null), payment(2, "15.00", " TX-99 ")), new BigDecimal("35.00"));

            assertThat(resolved).hasSize(2);
            assertThat(resolved.get(1).reference()).isEqualTo("TX-99");
            assertThat(resolved.getFirst().amount().scale()).isEqualTo(2);
        }

        @Test
        void resolve_rejects_whenSumDiffersFromTotal() {
            when(paymentMethodService.getById(1)).thenReturn(method(1, "EFECTIVO", false));
            // Pagar 50 por un total de 35 no se acepta: el vuelto no se registra
            assertThatThrownBy(() -> service().resolve(List.of(payment(1, "50", null)), new BigDecimal("35.00")))
                    .isInstanceOf(ValidationErrorException.class)
                    .hasMessageContaining("no coincide");
        }

        @Test
        void resolve_requiresReference_whenMethodDemandsIt() {
            when(paymentMethodService.getById(2)).thenReturn(method(2, "QR", true));
            assertThatThrownBy(() -> service().resolve(List.of(payment(2, "35", "  ")), new BigDecimal("35.00")))
                    .isInstanceOf(ValidationErrorException.class)
                    .hasMessageContaining("referencia");
        }

        @Test
        void resolve_rejectsInactiveMethod() {
            PaymentMethod inactive = method(1, "EFECTIVO", false);
            inactive.setActive(false);
            when(paymentMethodService.getById(1)).thenReturn(inactive);
            assertThatThrownBy(() -> service().resolve(List.of(payment(1, "35", null)), new BigDecimal("35.00")))
                    .isInstanceOf(ValidationErrorException.class);
        }

        @Test
        void register_savesPaidPayment_andSaleMovementPerPayment() {
            CashSession session = new CashSession();
            PaymentMethod cash = method(1, "EFECTIVO", false);
            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
                Payment p = inv.getArgument(0);
                p.setId(77);
                return p;
            });

            service().register(40, "#003", session, List.of(new ResolvedPayment(cash, new BigDecimal("35.00"), null)));

            ArgumentCaptor<Payment> payment = ArgumentCaptor.forClass(Payment.class);
            verify(paymentRepository).save(payment.capture());
            assertThat(payment.getValue().getStatus()).isEqualTo(PaymentStatusEnum.PAID);
            assertThat(payment.getValue().getOrderId()).isEqualTo(40);

            ArgumentCaptor<CashMovement> movement = ArgumentCaptor.forClass(CashMovement.class);
            verify(cashMovementRepository).save(movement.capture());
            assertThat(movement.getValue().getType()).isEqualTo(CashMovementTypeEnum.SALE);
            assertThat(movement.getValue().getPaymentId()).isEqualTo(77);
            assertThat(movement.getValue().getCashSession()).isSameAs(session);
            assertThat(movement.getValue().getDescription()).isEqualTo("Venta #003");
        }
    }

    @Nested
    class CashSessionServiceTest {
        @Mock CashSessionRepository cashSessionRepository;
        @Mock CashMovementRepository cashMovementRepository;
        @Mock UserService userService;
        @Mock JwtService jwtService;
        final Clock clock = Clock.fixed(Instant.parse("2026-11-10T12:00:00Z"), ZoneId.of("UTC"));

        CashSessionServiceImpl service() {
            return new CashSessionServiceImpl(cashSessionRepository, cashMovementRepository, new CashSessionMapper(),
                    userService, jwtService, clock);
        }

        private CashSession openSession() {
            UserApp user = new UserApp();
            user.setId(4);
            user.setUsername("maria.caja");
            CashSession s = new CashSession();
            s.setId(3);
            s.setUser(user);
            s.setStatus(CashSessionStatusEnum.OPEN);
            s.setOpeningAmount(new BigDecimal("100.00"));
            return s;
        }

        @Test
        void open_createsSession_andOpeningMovement() {
            UserApp user = new UserApp();
            user.setId(4);
            when(jwtService.getCurrentUserId()).thenReturn(4);
            when(cashSessionRepository.findOpenByUserId(4)).thenReturn(Optional.empty());
            when(userService.getById(4)).thenReturn(user);
            CashSessionOpenDto dto = new CashSessionOpenDto();
            dto.setOpeningAmount(new BigDecimal("100"));

            CashSessionPojo pojo = service().open(dto);

            ArgumentCaptor<CashMovement> movement = ArgumentCaptor.forClass(CashMovement.class);
            verify(cashMovementRepository).save(movement.capture());
            assertThat(movement.getValue().getType()).isEqualTo(CashMovementTypeEnum.OPENING);
            assertThat(movement.getValue().getAmount()).isEqualByComparingTo("100.00");
            assertThat(pojo.getStatus()).isEqualTo(CashSessionStatusEnum.OPEN);
            assertThat(pojo.getOpenedAt()).isEqualTo(LocalDateTime.now(clock));
            assertThat(pojo.getExpectedCash()).isEqualByComparingTo("100.00");
        }

        @Test
        void open_returnsConflict_whenUserAlreadyHasOpenSession() {
            when(jwtService.getCurrentUserId()).thenReturn(4);
            when(cashSessionRepository.findOpenByUserId(4)).thenReturn(Optional.of(openSession()));
            CashSessionOpenDto dto = new CashSessionOpenDto();
            dto.setOpeningAmount(BigDecimal.ZERO);

            assertThatThrownBy(() -> service().open(dto)).isInstanceOf(ConflictException.class);
            verify(cashSessionRepository, never()).saveAndFlush(any());
        }

        @Test
        void registerMovement_rejectsSaleType() {
            CashMovementDto dto = new CashMovementDto();
            dto.setType("SALE");
            dto.setAmount(BigDecimal.TEN);
            dto.setDescription("x");
            assertThatThrownBy(() -> service().registerMovement(dto)).isInstanceOf(ValidationErrorException.class);
        }

        @Test
        void registerMovement_requiresOpenSession() {
            when(jwtService.getCurrentUserId()).thenReturn(4);
            when(cashSessionRepository.findOpenByUserId(4)).thenReturn(Optional.empty());
            CashMovementDto dto = new CashMovementDto();
            dto.setType("EXPENSE");
            dto.setAmount(BigDecimal.TEN);
            dto.setDescription("Compra de hielo");
            assertThatThrownBy(() -> service().registerMovement(dto))
                    .isInstanceOf(ValidationErrorException.class)
                    .hasMessageContaining("abrir caja");
        }

        @Test
        void getCurrent_computesTotalsFromMovements() {
            CashSession session = openSession();
            when(jwtService.getCurrentUserId()).thenReturn(4);
            when(cashSessionRepository.findOpenByUserId(4)).thenReturn(Optional.of(session));
            LocalDateTime t = LocalDateTime.now(clock);
            when(cashMovementRepository.findPojoByCashSessionId(3)).thenReturn(List.of(
                    new CashMovementPojo(1, CashMovementTypeEnum.OPENING, null, null, null, new BigDecimal("100.00"), null, "Apertura", "m", t),
                    new CashMovementPojo(2, CashMovementTypeEnum.SALE, 1, "EFECTIVO", "Efectivo", new BigDecimal("35.00"), 40, "Venta #003", "m", t),
                    new CashMovementPojo(3, CashMovementTypeEnum.SALE, 2, "QR", "QR", new BigDecimal("20.00"), 41, "Venta #004", "m", t),
                    new CashMovementPojo(4, CashMovementTypeEnum.INCOME, null, null, null, new BigDecimal("10.00"), null, "Cambio", "m", t),
                    new CashMovementPojo(5, CashMovementTypeEnum.EXPENSE, null, null, null, new BigDecimal("4.50"), null, "Hielo", "m", t)));

            CashSessionPojo pojo = service().getCurrent().orElseThrow();

            assertThat(pojo.getSalesTotal()).isEqualByComparingTo("55.00");
            assertThat(pojo.getIncomeTotal()).isEqualByComparingTo("10.00");
            assertThat(pojo.getExpenseTotal()).isEqualByComparingTo("4.50");
            // 100 + 35 (solo efectivo) + 10 − 4.50
            assertThat(pojo.getExpectedCash()).isEqualByComparingTo("140.50");
            assertThat(pojo.getSalesByMethod()).hasSize(2);
        }

        @Test
        void getCurrent_isEmpty_whenNoOpenSession() {
            when(jwtService.getCurrentUserId()).thenReturn(4);
            when(cashSessionRepository.findOpenByUserId(4)).thenReturn(Optional.empty());
            assertThat(service().getCurrent()).isEmpty();
        }
    }
}
