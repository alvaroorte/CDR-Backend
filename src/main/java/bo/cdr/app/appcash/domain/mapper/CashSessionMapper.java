package bo.cdr.app.appcash.domain.mapper;

import bo.cdr.app.appcash.domain.entity.CashSession;
import bo.cdr.app.appcash.presentation.response.pojo.CashMethodTotalPojo;
import bo.cdr.app.appcash.presentation.response.pojo.CashMovementPojo;
import bo.cdr.app.appcash.presentation.response.pojo.CashSessionListPojo;
import bo.cdr.app.appcash.presentation.response.pojo.CashSessionPojo;
import bo.cdr.app.appcash.presentation.response.pojo.CashSummaryPojo;
import bo.cdr.app.common.MoneyUtil;
import bo.cdr.app.common.enums.CashMovementTypeEnum;
import bo.cdr.app.util.constants.PaymentMethodCodes;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Arma la vista de la caja y sus totales a partir de la bitácora de movimientos. */
@Component
public class CashSessionMapper {

    public CashSessionPojo toPojo(CashSession session, List<CashMovementPojo> movements) {
        CashSessionPojo pojo = new CashSessionPojo();
        pojo.setId(session.getId());
        pojo.setUserId(session.getUser().getId());
        pojo.setUsername(session.getUser().getUsername());
        pojo.setUserFullName(session.getUser().getFullName());
        pojo.setStatus(session.getStatus());
        pojo.setStatusLabel(session.getStatus().getLabel());
        pojo.setOpeningAmount(session.getOpeningAmount());
        pojo.setOpenedAt(session.getOpenedAt());
        pojo.setClosedAt(session.getClosedAt());
        pojo.setClosedBy(session.getClosedBy());
        pojo.setClosingNote(session.getClosingNote());
        pojo.setVersion(session.getVersion());
        pojo.setMovements(movements);

        BigDecimal sales = BigDecimal.ZERO;
        BigDecimal refunds = BigDecimal.ZERO;
        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expense = BigDecimal.ZERO;
        BigDecimal cashNetSales = BigDecimal.ZERO;
        Map<Integer, CashMethodTotalPojo> byMethod = new LinkedHashMap<>();

        for (CashMovementPojo m : movements) {
            switch (m.getType()) {
                case SALE, REFUND -> {
                    BigDecimal signed = m.getType() == CashMovementTypeEnum.SALE ? m.getAmount() : m.getAmount().negate();
                    if (m.getType() == CashMovementTypeEnum.SALE) {
                        sales = sales.add(m.getAmount());
                    } else {
                        refunds = refunds.add(m.getAmount());
                    }
                    if (PaymentMethodCodes.CASH.equals(m.getPaymentMethodCode())) {
                        cashNetSales = cashNetSales.add(signed);
                    }
                    byMethod.merge(m.getPaymentMethodId(),
                            new CashMethodTotalPojo(m.getPaymentMethodId(), m.getPaymentMethodCode(), m.getPaymentMethodName(), signed),
                            (a, b) -> new CashMethodTotalPojo(a.paymentMethodId(), a.paymentMethodCode(),
                                    a.paymentMethodName(), a.amount().add(b.amount())));
                }
                case INCOME -> income = income.add(m.getAmount());
                case EXPENSE -> expense = expense.add(m.getAmount());
                case OPENING -> { /* ya está en openingAmount */ }
            }
        }

        pojo.setSalesTotal(MoneyUtil.round(sales));
        pojo.setRefundsTotal(MoneyUtil.round(refunds));
        pojo.setIncomeTotal(MoneyUtil.round(income));
        pojo.setExpenseTotal(MoneyUtil.round(expense));
        pojo.setExpectedCash(MoneyUtil.round(session.getOpeningAmount().add(cashNetSales).add(income).subtract(expense)));
        pojo.setSalesByMethod(byMethod.values().stream()
                .map(t -> new CashMethodTotalPojo(t.paymentMethodId(), t.paymentMethodCode(), t.paymentMethodName(),
                        MoneyUtil.round(t.amount())))
                .toList());
        return pojo;
    }

    /**
     * Esperado por medio de pago (skill cdr-order-management §10): efectivo = apertura + ventas −
     * devoluciones + ingresos − egresos (los movimientos sin medio son efectivo); otros medios = ventas − devoluciones.
     */
    public Map<Integer, BigDecimal> expectedByMethod(BigDecimal openingAmount, List<CashMovementPojo> movements,
                                                     Integer cashMethodId) {
        Map<Integer, BigDecimal> expected = new LinkedHashMap<>();
        expected.put(cashMethodId, openingAmount);
        for (CashMovementPojo m : movements) {
            Integer methodId = m.getPaymentMethodId() == null ? cashMethodId : m.getPaymentMethodId();
            BigDecimal signed = switch (m.getType()) {
                case SALE, INCOME -> m.getAmount();
                case REFUND, EXPENSE -> m.getAmount().negate();
                case OPENING -> BigDecimal.ZERO;   // ya está en openingAmount
            };
            expected.merge(methodId, signed, BigDecimal::add);
        }
        expected.replaceAll((id, amount) -> MoneyUtil.round(amount));
        return expected;
    }

    /** Fila del historial; los totales del arqueo solo existen si la caja está cerrada. */
    public CashSessionListPojo toListPojo(CashSession session, List<CashSummaryPojo> summaries) {
        boolean closed = !summaries.isEmpty();
        BigDecimal expected = closed ? sum(summaries.stream().map(CashSummaryPojo::expectedAmount).toList()) : null;
        BigDecimal declared = closed ? sum(summaries.stream().map(CashSummaryPojo::declaredAmount).toList()) : null;
        BigDecimal difference = closed ? sum(summaries.stream().map(CashSummaryPojo::difference).toList()) : null;
        return new CashSessionListPojo(session.getId(), session.getUser().getId(), session.getUser().getUsername(),
                session.getUser().getFullName(), session.getStatus(), session.getStatus().getLabel(),
                session.getOpenedAt(), session.getClosedAt(), session.getClosedBy(), session.getOpeningAmount(),
                expected, declared, difference, session.getVersion());
    }

    private static BigDecimal sum(List<BigDecimal> values) {
        return MoneyUtil.round(values.stream().reduce(BigDecimal.ZERO, BigDecimal::add));
    }
}
