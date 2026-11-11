package bo.cdr.app.appcatalog.domain.service.implement;

import bo.cdr.app.appbase.data.repository.GenericRepository;
import bo.cdr.app.appbase.domain.service.CRUDServiceImpl;
import bo.cdr.app.appcatalog.data.ParameterRepository;
import bo.cdr.app.appcatalog.domain.entity.Parameter;
import bo.cdr.app.appcatalog.domain.service.interfaces.ParameterService;
import bo.cdr.app.appcatalog.presentation.response.pojo.ParameterPojo;
import bo.cdr.app.exception.EntityNotFoundException;
import bo.cdr.app.exception.UpdateNotAllowedException;
import bo.cdr.app.exception.response.ValidationErrorException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

/** Los parámetros los crea una migración; aquí solo se consultan y se edita su valor. */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParameterServiceImpl extends CRUDServiceImpl<Parameter, Integer> implements ParameterService {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final ParameterRepository parameterRepository;

    @Override
    protected GenericRepository<Parameter, Integer> getRepository() {
        return parameterRepository;
    }

    @Override
    public List<ParameterPojo> getAllParameterPojo() {
        return parameterRepository.findAllParameterPojo();
    }

    @Override
    public ParameterPojo getParameterPojoById(Integer id) {
        return parameterRepository.getParameterPojoById(id)
                .orElseThrow(() -> new EntityNotFoundException("Parámetro", id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ParameterPojo updateValue(Integer id, String value) {
        Parameter parameter = getById(id);
        if (!Boolean.TRUE.equals(parameter.getEditable())) {
            throw new UpdateNotAllowedException("El parámetro '" + parameter.getName() + "' no es editable.");
        }
        String normalized = normalize(parameter, value.trim());
        log.info("Parámetro {} cambiado de '{}' a '{}'", parameter.getCode(), parameter.getValue(), normalized);
        parameter.setValue(normalized);
        parameterRepository.save(parameter);
        return getParameterPojoById(id);
    }

    @Override
    public BigDecimal getDecimalValue(String code) {
        Parameter parameter = parameterRepository.findByCodeAndActiveIsTrue(code)
                .orElseThrow(() -> new EntityNotFoundException("Parámetro", "Código", code));
        return new BigDecimal(parameter.getValue());
    }

    /** Valida el valor según el tipo del parámetro y lo devuelve normalizado. */
    private String normalize(Parameter parameter, String value) {
        return switch (parameter.getValueType()) {
            case PERCENTAGE -> {
                BigDecimal number = parseNumber(value);
                if (number.compareTo(BigDecimal.ZERO) < 0 || number.compareTo(ONE_HUNDRED) > 0) {
                    throw new ValidationErrorException("El porcentaje debe estar entre 0 y 100.");
                }
                yield number.stripTrailingZeros().toPlainString();
            }
            case NUMBER -> parseNumber(value).stripTrailingZeros().toPlainString();
            case BOOLEAN -> {
                String lower = value.toLowerCase(Locale.ROOT);
                if (!lower.equals("true") && !lower.equals("false")) {
                    throw new ValidationErrorException("El valor debe ser 'true' o 'false'.");
                }
                yield lower;
            }
            case TEXT -> value;
        };
    }

    private BigDecimal parseNumber(String value) {
        try {
            return new BigDecimal(value.replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new ValidationErrorException("El valor debe ser numérico.");
        }
    }
}
