package bo.cdr.app.appreport.presentation.response.pojo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Cobros por medio de pago (PAID / REFUNDED) y cajas abiertas en el rango con su arqueo. */
public record PaymentsReportPojo(LocalDate dateFrom,
                                 LocalDate dateTo,
                                 List<PaymentMethod> paymentMethods,
                                 List<CashSession> cashSessions) {

    public record PaymentMethod(Integer paymentMethodId,
                                String paymentMethodName,
                                long paidCount,
                                BigDecimal paid,
                                long refundedCount,
                                BigDecimal refunded,
                                BigDecimal sharePercent) {
    }

    /** Totales del arqueo en null si la caja sigue abierta. */
    public record CashSession(Integer id,
                              String username,
                              String userFullName,
                              String status,
                              String statusLabel,
                              LocalDateTime openedAt,
                              LocalDateTime closedAt,
                              String closedBy,
                              BigDecimal openingAmount,
                              BigDecimal salesTotal,
                              BigDecimal expectedTotal,
                              BigDecimal declaredTotal,
                              BigDecimal differenceTotal) {
    }
}
