package bo.cdr.app.appcatalog.domain.service.interfaces;

import bo.cdr.app.appbase.domain.service.CRUDService;
import bo.cdr.app.appcatalog.domain.entity.PaymentMethod;
import bo.cdr.app.appcatalog.presentation.request.dto.PaymentMethodDto;
import bo.cdr.app.appcatalog.presentation.response.pojo.PaymentMethodPojo;

import java.util.List;

public interface PaymentMethodService extends CRUDService<PaymentMethod, Integer> {
    PaymentMethod create(PaymentMethodDto dto);
    PaymentMethod update(Integer id, PaymentMethodDto dto);
    List<PaymentMethodPojo> getAllPaymentMethodPojo();
    PaymentMethodPojo getPaymentMethodPojoById(Integer id);
    /** Medio de efectivo (código EFECTIVO): apertura, ingresos y egresos de caja se cuentan en él. */
    PaymentMethod getCashMethod();
    /** Medios activos más los indicados (aunque estén inactivos), para el arqueo de una caja. */
    List<PaymentMethod> getActiveIncluding(java.util.Collection<Integer> ids);
}
