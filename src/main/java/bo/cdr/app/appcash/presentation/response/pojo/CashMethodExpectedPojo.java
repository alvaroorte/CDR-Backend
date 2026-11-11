package bo.cdr.app.appcash.presentation.response.pojo;

import java.math.BigDecimal;

/** Lo que el sistema espera encontrar por medio de pago al cerrar. {@code cash}: incluye apertura, ingresos y egresos. */
public record CashMethodExpectedPojo(Integer paymentMethodId, String paymentMethodCode, String paymentMethodName,
                                     boolean cash, BigDecimal expectedAmount) {
}
