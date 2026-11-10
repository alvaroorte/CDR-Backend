package bo.cdr.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Reloj inyectable para la lógica que depende de la hora actual, de modo que los tests puedan
 * fijarla. Usa la zona por defecto de la JVM, igual que los {@code LocalDateTime.now()} del resto
 * del backend.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
