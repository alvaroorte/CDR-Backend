package bo.cdr.app.appcash.presentation.response.pojo;

import java.math.BigDecimal;

/** Arqueo por medio de pago. {@code difference} = declarado - esperado (negativo = faltante). Record sin @Pojo. */
public record CashSummaryPojo(Integer cashSessionId, Integer paymentMethodId, String paymentMethodCode,
                              String paymentMethodName, BigDecimal expectedAmount, BigDecimal declaredAmount,
                              BigDecimal difference) {
}
