package bo.cdr.app.integration;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Base de las pruebas de integración (Etapa 7): aplicación completa (seguridad, transacciones,
 * bloqueos, Flyway) contra un PostgreSQL 15 desechable en Docker. Las llamadas van por HTTP real,
 * igual que el frontend. Un solo contenedor por ejecución (patrón singleton): el contexto de Spring
 * se reutiliza entre clases, así que cada prueba crea sus propios datos con nombres únicos.
 * <p>
 * Se ejecutan con {@code .\gradlew.bat integrationTest} (requiere Docker encendido).
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.profiles.active=dev",
        "app.admin.initial-password=Admin123*",
        "app.login.rate-limit.enabled=false",
        "logging.file.path=build/it-logs"
})
public abstract class IntegrationTestSupport {

    protected static final String ADMIN_PASSWORD = "Admin123*";
    protected static final String USER_PASSWORD = "Prueba2026!";

    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:15-alpine");

    static {
        POSTGRES.start();
    }

    @LocalServerPort
    int port;

    protected final JsonMapper json = JsonMapper.builder().build();
    private final HttpClient http = HttpClient.newHttpClient();

    /** Respuesta HTTP con el cuerpo como árbol JSON (null si vino vacío). */
    protected record Resp(int status, JsonNode body) {
        public int id() {
            return body.get("id").asInt();
        }
    }

    // ------------------------------------------------------------------ HTTP

    protected Resp call(String token, String method, String path, Object body) {
        try {
            HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/cdr" + path))
                    .header("Content-Type", "application/json")
                    .method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                            : HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body), StandardCharsets.UTF_8));
            if (token != null) {
                request.header("Authorization", "Bearer " + token);
            }
            HttpResponse<String> response = http.send(request.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            String text = response.body();
            return new Resp(response.statusCode(), text == null || text.isBlank() ? null : json.readTree(text));
        } catch (Exception e) {
            throw new IllegalStateException(method + " " + path + " falló", e);
        }
    }

    protected Resp get(String token, String path) {
        return call(token, "GET", path, null);
    }

    protected Resp post(String token, String path, Object body) {
        return call(token, "POST", path, body);
    }

    protected String login(String username, String password) {
        Resp r = post(null, "/login", Map.of("username", username,
                "password", Base64.getEncoder().encodeToString(password.getBytes(StandardCharsets.UTF_8))));
        if (r.status() != 200) {
            throw new IllegalStateException("login " + username + " → " + r.status() + " " + r.body());
        }
        return r.body().get("token").asString();
    }

    protected String admin() {
        return login("admin", ADMIN_PASSWORD);
    }

    // ------------------------------------------------------------------ datos de prueba

    protected static String unique(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /** Crea un usuario con el rol indicado (código) y devuelve su token. */
    protected String newUser(String admin, String roleCode) {
        int roleId = -1;
        JsonNode roles = get(admin, "/roles").body();
        for (int i = 0; i < roles.size(); i++) {
            if (roleCode.equals(roles.get(i).get("code").asString())) {
                roleId = roles.get(i).get("id").asInt();
            }
        }
        String username = unique(roleCode.toLowerCase()).replace("-", ".");
        Resp r = post(admin, "/users", Map.of("username", username, "fullName", "Usuario " + username,
                "password", USER_PASSWORD, "roleIds", List.of(roleId), "active", true));
        if (r.status() != 201) {
            throw new IllegalStateException("usuario: " + r.status() + " " + r.body());
        }
        return login(username, USER_PASSWORD);
    }

    /** Categoría + producto disponible; devuelve el id del producto. */
    protected int newProduct(String admin, String price) {
        Resp category = post(admin, "/category", Map.of("name", unique("Cat"), "ranking", 1, "active", true));
        Resp product = post(admin, "/product", Map.of("categoryId", category.id(), "name", unique("Prod"),
                "price", new BigDecimal(price), "available", true, "active", true, "addonIds", List.of()));
        if (product.status() != 201) {
            throw new IllegalStateException("producto: " + product.status() + " " + product.body());
        }
        return product.id();
    }

    /** Número de mesa libre con un número alto aleatorio (no choca entre clases). */
    protected int newTableMarker(String admin) {
        int number = 1000 + (int) (Math.random() * 1_000_000);
        Resp r = post(admin, "/table-marker", Map.of("number", number, "active", true));
        if (r.status() != 201) {
            throw new IllegalStateException("mesa: " + r.status() + " " + r.body());
        }
        return r.id();
    }

    protected int paymentMethodId(String token, String code) {
        JsonNode methods = get(token, "/payment-method").body();
        for (int i = 0; i < methods.size(); i++) {
            if (code.equals(methods.get(i).get("code").asString())) {
                return methods.get(i).get("id").asInt();
            }
        }
        throw new IllegalStateException("Medio de pago " + code + " no encontrado");
    }

    protected Resp openCash(String token, String amount) {
        return post(token, "/cash-session/open", Map.of("openingAmount", new BigDecimal(amount)));
    }

    /** Pedido para llevar de una unidad, pagado completo con el medio indicado. */
    protected Resp takeaway(String token, int productId, String price, int paymentMethodId) {
        return post(token, "/order", Map.of("serviceType", "TAKEAWAY",
                "items", List.of(Map.of("productId", productId, "quantity", 1)),
                "payments", List.of(Map.of("paymentMethodId", paymentMethodId, "amount", new BigDecimal(price)))));
    }

    protected Resp dineIn(String token, int productId, String price, int paymentMethodId, int tableMarkerId) {
        return post(token, "/order", Map.of("serviceType", "DINE_IN", "tableMarkerId", tableMarkerId,
                "items", List.of(Map.of("productId", productId, "quantity", 1)),
                "payments", List.of(Map.of("paymentMethodId", paymentMethodId, "amount", new BigDecimal(price)))));
    }

    // ------------------------------------------------------------------ concurrencia

    /**
     * Ejecuta las tareas a la vez: todos los hilos esperan en una barrera y salen juntos, para que las
     * peticiones lleguen al backend prácticamente en el mismo instante.
     */
    protected <T> List<T> concurrently(List<Callable<T>> tasks) {
        ExecutorService pool = Executors.newFixedThreadPool(tasks.size());
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<T>> futures = new ArrayList<>();
            for (Callable<T> task : tasks) {
                futures.add(pool.submit(() -> {
                    start.await();
                    return task.call();
                }));
            }
            start.countDown();
            List<T> results = new ArrayList<>();
            for (Future<T> future : futures) {
                results.add(future.get(60, TimeUnit.SECONDS));
            }
            return results;
        } catch (Exception e) {
            throw new IllegalStateException("Ejecución concurrente falló", e);
        } finally {
            pool.shutdownNow();
        }
    }

    protected static long countStatus(List<Resp> responses, int status) {
        return responses.stream().filter(r -> r.status() == status).count();
    }
}
