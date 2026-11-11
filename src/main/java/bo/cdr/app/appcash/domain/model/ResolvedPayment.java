package bo.cdr.app.appcash.domain.model;

import bo.cdr.app.appcatalog.domain.entity.PaymentMethod;

import java.math.BigDecimal;

/** Pago ya validado (medio activo, referencia, importe redondeado), listo para grabarse. */
public record ResolvedPayment(PaymentMethod paymentMethod, BigDecimal amount, String reference) {
}
