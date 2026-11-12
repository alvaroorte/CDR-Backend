package bo.cdr.app.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Concurrencia real (skill cdr-order-management §6, §8, §10): peticiones simultáneas por HTTP contra
 * PostgreSQL. Cada prueba verifica que "gana uno" y que los datos quedan consistentes.
 */
class ConcurrencyIT extends IntegrationTestSupport {

    private static final String PRICE = "15.50";

    private String admin;
    private int productId;
    private int cash;
    private int qr;

    @BeforeEach
    void setUp() {
        admin = admin();
        productId = newProduct(admin, PRICE);
        cash = paymentMethodId(admin, "EFECTIVO");
        qr = paymentMethodId(admin, "QR");
    }

    private String cashierWithOpenCash() {
        String cashier = newUser(admin, "CAJERO");
        assertThat(openCash(cashier, "100").status()).isEqualTo(201);
        return cashier;
    }

    private Resp patchStatus(String token, int orderId, String toStatus, long version) {
        return call(token, "PATCH", "/order/" + orderId + "/status", Map.of("toStatus", toStatus, "expectedVersion", version));
    }

    // ------------------------------------------------------------------ número de mesa

    @Test
    void sameTableMarker_twoCashiersAtOnce_onlyOneGetsIt() {
        int marker = newTableMarker(admin);
        String c1 = cashierWithOpenCash();
        String c2 = cashierWithOpenCash();

        List<Resp> results = concurrently(List.of(
                () -> dineIn(c1, productId, PRICE, cash, marker),
                () -> dineIn(c2, productId, PRICE, cash, marker)));

        assertThat(countStatus(results, 201)).as("un solo pedido con el número").isEqualTo(1);
        assertThat(countStatus(results, 409)).as("el otro recibe 'número en uso'").isEqualTo(1);
        JsonNode availability = get(c1, "/table-marker/availability").body();
        for (int i = 0; i < availability.size(); i++) {
            if (availability.get(i).get("id").asInt() == marker) {
                assertThat(availability.get(i).get("inUse").asBoolean()).isTrue();
            }
        }
    }

    // ------------------------------------------------------------------ código diario

    @Test
    void dailyCode_manyConcurrentOrders_neverRepeatsNorSkips() {
        List<String> cashiers = List.of(cashierWithOpenCash(), cashierWithOpenCash(), cashierWithOpenCash());
        List<Callable<Resp>> tasks = new ArrayList<>();
        for (int i = 0; i < 18; i++) {
            String cashier = cashiers.get(i % cashiers.size());
            tasks.add(() -> takeaway(cashier, productId, PRICE, cash));
        }

        List<Resp> results = concurrently(tasks);

        assertThat(countStatus(results, 201)).isEqualTo(18);
        Set<Integer> codes = new HashSet<>();
        results.forEach(r -> codes.add(r.body().get("code").asInt()));
        assertThat(codes).as("códigos únicos").hasSize(18);
        int min = codes.stream().min(Integer::compare).orElseThrow();
        int max = codes.stream().max(Integer::compare).orElseThrow();
        assertThat(max - min + 1).as("sin huecos").isEqualTo(18);
    }

    // ------------------------------------------------------------------ caja

    @Test
    void sameCashier_parallelSales_allSucceed_andCashTotalsAddUp() {
        String cashier = cashierWithOpenCash();
        List<Callable<Resp>> tasks = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            tasks.add(() -> takeaway(cashier, productId, PRICE, cash));
        }

        List<Resp> results = concurrently(tasks);

        assertThat(countStatus(results, 201)).as("los cobros del mismo cajero se serializan, no fallan").isEqualTo(10);
        JsonNode session = get(cashier, "/cash-session/current").body();
        assertThat(new BigDecimal(session.get("salesTotal").asString())).isEqualByComparingTo("155.00");
        assertThat(new BigDecimal(session.get("expectedCash").asString())).isEqualByComparingTo("255.00");
    }

    @Test
    void openCash_twiceAtOnce_onlyOneSessionIsOpened() {
        String cashier = newUser(admin, "CAJERO");

        List<Resp> results = concurrently(List.of(() -> openCash(cashier, "50"), () -> openCash(cashier, "50")));

        assertThat(countStatus(results, 201)).isEqualTo(1);
        assertThat(countStatus(results, 409)).isEqualTo(1);
    }

    @Test
    void closeWhileSelling_neverClosesWithAStaleCount() {
        for (int round = 0; round < 3; round++) {
            String cashier = cashierWithOpenCash();
            assertThat(takeaway(cashier, productId, PRICE, cash).status()).isEqualTo(201);
            int sessionId = get(cashier, "/cash-session/current").id();
            JsonNode preview = get(cashier, "/cash-session/" + sessionId + "/close-preview").body();
            List<Map<String, Object>> declared = new ArrayList<>();
            JsonNode methods = preview.get("methods");
            for (int i = 0; i < methods.size(); i++) {
                declared.add(Map.of("paymentMethodId", methods.get(i).get("paymentMethodId").asInt(),
                        "amount", new BigDecimal(methods.get(i).get("expectedAmount").asString())));
            }
            long version = preview.get("version").asLong();

            List<Resp> results = concurrently(List.of(
                    () -> post(cashier, "/cash-session/" + sessionId + "/close", Map.of("expectedVersion", version, "declared", declared)),
                    () -> takeaway(cashier, productId, PRICE, cash)));
            Resp close = results.get(0);
            Resp sale = results.get(1);

            assertThat(close.status() == 200 && sale.status() == 201)
                    .as("no puede cerrarse con un arqueo que no incluye una venta registrada (ronda %d: cierre %d, venta %d)",
                            round, close.status(), sale.status())
                    .isFalse();
            if (close.status() == 200) {
                assertThat(sale.status()).as("la venta posterior al cierre no encuentra caja abierta").isEqualTo(400);
            } else {
                assertThat(close.status()).as("arqueo desactualizado").isEqualTo(409);
                assertThat(sale.status()).isEqualTo(201);
            }
        }
    }

    // ------------------------------------------------------------------ estados

    @Test
    void twoKitchenUsers_sameOrderSameVersion_onlyOneTransitionApplies() {
        String cashier = cashierWithOpenCash();
        String kitchen1 = newUser(admin, "COCINA");
        String kitchen2 = newUser(admin, "COCINA");
        Resp order = takeaway(cashier, productId, PRICE, cash);
        long version = order.body().get("version").asLong();

        List<Resp> results = concurrently(List.of(
                () -> patchStatus(kitchen1, order.id(), "IN_PREPARATION", version),
                () -> patchStatus(kitchen2, order.id(), "IN_PREPARATION", version)));

        assertThat(countStatus(results, 200)).isEqualTo(1);
        assertThat(countStatus(results, 409)).isEqualTo(1);
        JsonNode detail = get(cashier, "/order/" + order.id()).body();
        assertThat(detail.get("statusHistory").size()).as("creación + un solo cambio").isEqualTo(2);
    }

    @Test
    void deliverAndCancelAtOnce_exactlyOneWins_andPaymentsMatchTheResult() {
        String cashier = cashierWithOpenCash();
        Resp adminCash = openCash(admin, "0");
        assertThat(adminCash.status()).isIn(201, 409); // el ADMIN puede tener ya su caja abierta
        String kitchen = newUser(admin, "COCINA");
        Resp order = post(cashier, "/order", Map.of("serviceType", "TAKEAWAY",
                "items", List.of(Map.of("productId", productId, "quantity", 1)),
                "payments", List.of(Map.of("paymentMethodId", qr, "amount", new BigDecimal(PRICE), "reference", "TX-IT"))));
        Resp prep = patchStatus(kitchen, order.id(), "IN_PREPARATION", order.body().get("version").asLong());
        Resp ready = patchStatus(kitchen, order.id(), "READY", prep.body().get("version").asLong());
        long version = ready.body().get("version").asLong();

        List<Resp> results = concurrently(List.of(
                () -> patchStatus(cashier, order.id(), "DELIVERED", version),
                () -> post(admin, "/order/" + order.id() + "/cancel", Map.of("reason", "Prueba de concurrencia", "expectedVersion", version))));

        assertThat(countStatus(results, 200)).as("gana uno").isEqualTo(1);
        assertThat(countStatus(results, 409)).isEqualTo(1);
        JsonNode detail = get(admin, "/order/" + order.id()).body();
        String status = detail.get("status").asString();
        String paymentStatus = detail.get("payments").get(0).get("status").asString();
        if ("DELIVERED".equals(status)) {
            assertThat(paymentStatus).isEqualTo("PAID");
        } else {
            assertThat(status).isEqualTo("CANCELLED");
            assertThat(paymentStatus).isEqualTo("REFUNDED");
        }
    }
}
