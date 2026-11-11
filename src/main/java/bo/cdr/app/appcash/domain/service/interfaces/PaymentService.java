package bo.cdr.app.appcash.domain.service.interfaces;

import bo.cdr.app.appbase.domain.service.CRUDService;
import bo.cdr.app.appcash.domain.entity.CashSession;
import bo.cdr.app.appcash.domain.entity.Payment;
import bo.cdr.app.appcash.domain.model.ResolvedPayment;
import bo.cdr.app.appcash.presentation.request.dto.PaymentDto;
import bo.cdr.app.appcash.presentation.response.pojo.PaymentPojo;

import java.math.BigDecimal;
import java.util.List;

public interface PaymentService extends CRUDService<Payment, Integer> {

    /**
     * Valida los pagos de un pedido ANTES de grabarlo: medio activo, referencia cuando el medio la
     * exige y Σ importes == total exacto. No persiste nada.
     */
    List<ResolvedPayment> resolve(List<PaymentDto> payments, BigDecimal orderTotal);

    /** Graba los pagos (PAID) y un movimiento SALE por pago en la caja indicada. */
    List<PaymentPojo> register(Integer orderId, String orderLabel, CashSession cashSession, List<ResolvedPayment> payments);

    List<PaymentPojo> getByOrderId(Integer orderId);

    /**
     * Cancelación: pagos PAID → REFUNDED y un movimiento REFUND (egreso) por pago, con su mismo
     * medio e importe, en la caja indicada (la de quien cancela).
     */
    List<PaymentPojo> refundOrderPayments(Integer orderId, String orderLabel, CashSession cashSession);
}
