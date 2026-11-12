package bo.cdr.app.integration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Flyway desde una base vacía: todas las migraciones se aplican en orden, Hibernate valida el
 * esquema ({@code ddl-auto=validate}: si no coincidiera, el contexto no arrancaría) y existen las
 * garantías de concurrencia que viven en la BD.
 */
class MigrationIT extends IntegrationTestSupport {

    @Autowired Flyway flyway;
    @Autowired JdbcTemplate jdbc;

    @Test
    void allMigrations_applyFromEmptyDatabase_inOrder() throws IOException {
        int files = new PathMatchingResourcePatternResolver().getResources("classpath:db/migration/V*__*.sql").length;
        MigrationInfo[] applied = flyway.info().applied();

        assertThat(applied).hasSize(files);
        assertThat(Arrays.stream(applied).allMatch(m -> m.getState().isApplied() && !m.getState().isFailed())).isTrue();
        assertThat(flyway.info().pending()).isEmpty();
    }

    @Test
    void concurrencyGuarantees_existAsPartialUniqueIndexes() {
        List<String> indexes = jdbc.queryForList(
                "SELECT indexname FROM pg_indexes WHERE indexname IN ('ux_orders_marker_open', 'ux_cash_sessions_user_open')",
                String.class);
        assertThat(indexes).containsExactlyInAnyOrder("ux_orders_marker_open", "ux_cash_sessions_user_open");
    }

    @Test
    void seedData_rolesAndPaymentMethods() {
        assertThat(jdbc.queryForList("SELECT code FROM security.role", String.class))
                .contains("ADMIN", "CAJERO", "COCINA");
        assertThat(jdbc.queryForList("SELECT code FROM payment_methods", String.class))
                .contains("EFECTIVO", "QR");
    }
}
