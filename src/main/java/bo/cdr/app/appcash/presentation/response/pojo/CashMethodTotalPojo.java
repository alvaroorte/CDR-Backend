package bo.cdr.app.appcash.presentation.response.pojo;

import java.math.BigDecimal;

/**
 * Ventas netas (ventas − devoluciones) de una caja por medio de pago.
 * Sin @Pojo: esa anotación registra un @Component y un record no tiene constructor sin argumentos.
 */
public record CashMethodTotalPojo(Integer paymentMethodId, String paymentMethodCode, String paymentMethodName,
                                  BigDecimal amount) {
}
