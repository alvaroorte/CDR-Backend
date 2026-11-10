package bo.cdr.app.util;

import bo.cdr.app.exception.ErrorHandler;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorFactory;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Utility for building MockMvc in standaloneSetup mode.
 * Uses Jackson 3 (JsonMapper) with Java time support built-in (no JavaTimeModule needed).
 */
public final class MockMvcTestSupport {

    private MockMvcTestSupport() {}

    public static MockMvc buildMockMvc(Object... controllers) {
        return buildMockMvc(NoOpConstraintValidatorFactory.INSTANCE, controllers);
    }

    public static MockMvc buildMockMvc(ConstraintValidatorFactory validatorFactory, Object... controllers) {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.setConstraintValidatorFactory(validatorFactory);
        validator.afterPropertiesSet();

        JsonMapper jsonMapper = JsonMapper.builder()
                .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();

        return MockMvcBuilders.standaloneSetup(controllers)
                .setControllerAdvice(new ErrorHandler())
                .setValidator(validator)
                .setMessageConverters(
                        new ResourceHttpMessageConverter(),
                        new JacksonJsonHttpMessageConverter(jsonMapper))
                .build();
    }

    /**
     * Default constraint validator factory that uses reflection (no Spring context).
     * Handles only validators with no-arg constructors.
     */
    public static final class NoOpConstraintValidatorFactory implements ConstraintValidatorFactory {

        public static final NoOpConstraintValidatorFactory INSTANCE = new NoOpConstraintValidatorFactory();

        @Override
        public <T extends ConstraintValidator<?, ?>> T getInstance(Class<T> key) {
            try {
                return key.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException("Cannot instantiate validator: " + key.getName()
                        + ". Use ConstraintValidatorFactory with Spring bean support.", e);
            }
        }

        @Override
        public void releaseInstance(ConstraintValidator<?, ?> instance) {}
    }
}
