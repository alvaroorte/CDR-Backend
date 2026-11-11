package bo.cdr.app.appcatalog.domain.service.interfaces;

import bo.cdr.app.appbase.domain.service.CRUDService;
import bo.cdr.app.appcatalog.domain.entity.Parameter;
import bo.cdr.app.appcatalog.presentation.response.pojo.ParameterPojo;

import java.math.BigDecimal;
import java.util.List;

public interface ParameterService extends CRUDService<Parameter, Integer> {
    List<ParameterPojo> getAllParameterPojo();
    ParameterPojo getParameterPojoById(Integer id);
    ParameterPojo updateValue(Integer id, String value);
    /** Valor numérico de un parámetro activo por código (p. ej. CASHIER_MAX_DISCOUNT_PERCENT). */
    BigDecimal getDecimalValue(String code);
}
