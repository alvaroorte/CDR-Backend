package bo.cdr.app.appcatalog.domain.mapper;

import bo.cdr.app.appcatalog.domain.entity.PaymentMethod;
import bo.cdr.app.appcatalog.presentation.request.dto.PaymentMethodDto;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class PaymentMethodMapper {
    public PaymentMethod fromDto(PaymentMethodDto dto, PaymentMethod found) {
        PaymentMethod method = found != null ? found : new PaymentMethod();
        if (found == null) {
            method.setCode(dto.getCode().trim().toUpperCase(Locale.ROOT)); // el código no cambia al editar
        }
        method.setName(dto.getName().trim());
        method.setRequiresReference(dto.getRequiresReference());
        method.setGivesChange(dto.getGivesChange());
        method.setActive(dto.getActive());
        return method;
    }
}
