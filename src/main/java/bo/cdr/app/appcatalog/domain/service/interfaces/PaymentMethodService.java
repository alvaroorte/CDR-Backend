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
}
