package bo.cdr.app.appcash.domain.service.implement;

import bo.cdr.app.appbase.data.repository.GenericRepository;
import bo.cdr.app.appbase.domain.service.CRUDServiceImpl;
import bo.cdr.app.appcash.data.CashMovementRepository;
import bo.cdr.app.appcash.data.PaymentRepository;
import bo.cdr.app.appcash.domain.entity.CashMovement;
import bo.cdr.app.appcash.domain.entity.CashSession;
import bo.cdr.app.appcash.domain.entity.Payment;
import bo.cdr.app.appcash.domain.model.ResolvedPayment;
import bo.cdr.app.appcash.domain.service.interfaces.PaymentService;
import bo.cdr.app.appcash.presentation.request.dto.PaymentDto;
import bo.cdr.app.appcash.presentation.response.pojo.PaymentPojo;
import bo.cdr.app.appcatalog.domain.entity.PaymentMethod;
import bo.cdr.app.appcatalog.domain.service.interfaces.PaymentMethodService;
import bo.cdr.app.common.MoneyUtil;
import bo.cdr.app.common.enums.CashMovementTypeEnum;
import bo.cdr.app.common.enums.PaymentStatusEnum;
import bo.cdr.app.exception.response.ValidationErrorException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Pagos al registrar el pedido (skill cdr-order-management §7): varios por pedido, Σ == total,
 * sin guardar recibido ni vuelto; cada pago genera un movimiento SALE en la caja de quien cobra.
 */
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl extends CRUDServiceImpl<Payment, Integer> implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final CashMovementRepository cashMovementRepository;
    private final PaymentMethodService paymentMethodService;

    @Override
    protected GenericRepository<Payment, Integer> getRepository() {
        return paymentRepository;
    }

    @Override
    public List<ResolvedPayment> resolve(List<PaymentDto> payments, BigDecimal orderTotal) {
        if (payments == null || payments.isEmpty()) {
            throw new ValidationErrorException("Debe registrar al menos un pago.");
        }
        List<ResolvedPayment> resolved = new ArrayList<>();
        BigDecimal sum = BigDecimal.ZERO;
        for (PaymentDto dto : payments) {
            PaymentMethod method = paymentMethodService.getById(dto.getPaymentMethodId());
            if (!Boolean.TRUE.equals(method.getActive())) {
                throw new ValidationErrorException("El medio de pago '" + method.getName() + "' está inactivo.");
            }
            String reference = dto.getReference() == null || dto.getReference().isBlank() ? null : dto.getReference().trim();
            if (Boolean.TRUE.equals(method.getRequiresReference()) && reference == null) {
                throw new ValidationErrorException("El pago con '" + method.getName() + "' exige un número de referencia.");
            }
            BigDecimal amount = MoneyUtil.round(dto.getAmount());
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ValidationErrorException("El importe de cada pago debe ser mayor a 0.");
            }
            sum = sum.add(amount);
            resolved.add(new ResolvedPayment(method, amount, reference));
        }
        if (sum.compareTo(orderTotal) != 0) {
            throw new ValidationErrorException(String.format(
                    "La suma de los pagos (Bs %s) no coincide con el total del pedido (Bs %s).",
                    MoneyUtil.round(sum).toPlainString(), MoneyUtil.round(orderTotal).toPlainString()));
        }
        return resolved;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<PaymentPojo> register(Integer orderId, String orderLabel, CashSession cashSession,
                                      List<ResolvedPayment> payments) {
        for (ResolvedPayment resolved : payments) {
            Payment payment = new Payment();
            payment.setOrderId(orderId);
            payment.setCashSession(cashSession);
            payment.setPaymentMethod(resolved.paymentMethod());
            payment.setAmount(resolved.amount());
            payment.setReference(resolved.reference());
            payment.setStatus(PaymentStatusEnum.PAID);
            paymentRepository.save(payment);

            CashMovement sale = new CashMovement();
            sale.setCashSession(cashSession);
            sale.setType(CashMovementTypeEnum.SALE);
            sale.setPaymentMethod(resolved.paymentMethod());
            sale.setAmount(resolved.amount());
            sale.setOrderId(orderId);
            sale.setPaymentId(payment.getId());
            sale.setDescription("Venta " + orderLabel);
            cashMovementRepository.save(sale);
        }
        return getByOrderId(orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<PaymentPojo> refundOrderPayments(Integer orderId, String orderLabel, CashSession cashSession) {
        for (Payment payment : paymentRepository.findByOrderIdAndStatusOrderByIdAsc(orderId, PaymentStatusEnum.PAID)) {
            payment.setStatus(PaymentStatusEnum.REFUNDED);
            paymentRepository.save(payment);

            CashMovement refund = new CashMovement();
            refund.setCashSession(cashSession);
            refund.setType(CashMovementTypeEnum.REFUND);
            refund.setPaymentMethod(payment.getPaymentMethod());
            refund.setAmount(payment.getAmount());
            refund.setOrderId(orderId);
            refund.setPaymentId(payment.getId());
            refund.setDescription("Devolución " + orderLabel);
            cashMovementRepository.save(refund);
        }
        return getByOrderId(orderId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentPojo> getByOrderId(Integer orderId) {
        return paymentRepository.findPojoByOrderId(orderId);
    }
}
