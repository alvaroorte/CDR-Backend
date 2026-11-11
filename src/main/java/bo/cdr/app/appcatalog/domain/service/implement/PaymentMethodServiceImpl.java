package bo.cdr.app.appcatalog.domain.service.implement;

import bo.cdr.app.appbase.data.repository.GenericRepository;
import bo.cdr.app.appbase.domain.service.CRUDServiceImpl;
import bo.cdr.app.appcatalog.data.PaymentMethodRepository;
import bo.cdr.app.appcatalog.domain.entity.PaymentMethod;
import bo.cdr.app.appcatalog.domain.mapper.PaymentMethodMapper;
import bo.cdr.app.appcatalog.domain.service.interfaces.PaymentMethodService;
import bo.cdr.app.appcatalog.presentation.request.dto.PaymentMethodDto;
import bo.cdr.app.appcatalog.presentation.response.pojo.PaymentMethodPojo;
import bo.cdr.app.exception.DuplicateException;
import bo.cdr.app.exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentMethodServiceImpl extends CRUDServiceImpl<PaymentMethod, Integer> implements PaymentMethodService {

    private final PaymentMethodRepository paymentMethodRepository;
    private final PaymentMethodMapper paymentMethodMapper;

    @Override
    protected GenericRepository<PaymentMethod, Integer> getRepository() {
        return paymentMethodRepository;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentMethod create(PaymentMethodDto dto) {
        if (paymentMethodRepository.existsByCodeIgnoreCase(dto.getCode().trim())) {
            throw new DuplicateException("Medio de pago", "Código", dto.getCode().trim());
        }
        validateDuplicateName(dto, null);
        return paymentMethodRepository.save(paymentMethodMapper.fromDto(dto, null));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentMethod update(Integer id, PaymentMethodDto dto) {
        PaymentMethod found = getById(id);
        validateDuplicateName(dto, id);
        return paymentMethodRepository.save(paymentMethodMapper.fromDto(dto, found));
    }

    @Override
    public List<PaymentMethodPojo> getAllPaymentMethodPojo() {
        return paymentMethodRepository.findAllPaymentMethodPojo();
    }

    @Override
    public PaymentMethodPojo getPaymentMethodPojoById(Integer id) {
        return paymentMethodRepository.getPaymentMethodPojoById(id)
                .orElseThrow(() -> new EntityNotFoundException("Medio de pago", id));
    }

    private void validateDuplicateName(PaymentMethodDto dto, Integer excludeId) {
        if (!Boolean.TRUE.equals(dto.getActive())) {
            return;
        }
        String name = dto.getName().trim();
        boolean exists = excludeId == null
                ? paymentMethodRepository.existsByNameIgnoreCaseAndActiveIsTrue(name)
                : paymentMethodRepository.existsByNameIgnoreCaseAndActiveIsTrueAndIdNot(name, excludeId);
        if (exists) {
            throw new DuplicateException("Medio de pago", "Nombre", name);
        }
    }
}
