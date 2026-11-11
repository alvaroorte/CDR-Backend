package bo.cdr.app.apporder.domain.model;

import bo.cdr.app.common.enums.OrderStatusEnum;
import bo.cdr.app.util.constants.RoleCodes;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static bo.cdr.app.common.enums.OrderStatusEnum.*;

/**
 * Transiciones permitidas y quién puede hacerlas (skill cdr-order-management §8).
 * Decisión de la Etapa 4: COCINA y ADMIN pueden retroceder UN paso (LISTO → EN_PREPARACION →
 * PENDIENTE) para corregir toques accidentales. ENTREGADO y CANCELADO son finales.
 * La cancelación no pasa por aquí: tiene su propio flujo (motivo + devolución).
 */
public final class OrderStateMachine {

    private static final Set<String> KITCHEN = Set.of(RoleCodes.COCINA, RoleCodes.ADMIN);
    private static final Set<String> CASHIER = Set.of(RoleCodes.CAJERO, RoleCodes.ADMIN);

    private static final Map<OrderStatusEnum, Map<OrderStatusEnum, Set<String>>> TRANSITIONS = Map.of(
            PENDING, Map.of(IN_PREPARATION, KITCHEN),
            IN_PREPARATION, Map.of(READY, KITCHEN, PENDING, KITCHEN),
            READY, Map.of(DELIVERED, CASHIER, IN_PREPARATION, KITCHEN));

    private OrderStateMachine() {}

    /** true si la transición existe (sin mirar roles). */
    public static boolean exists(OrderStatusEnum from, OrderStatusEnum to) {
        return TRANSITIONS.getOrDefault(from, Map.of()).containsKey(to);
    }

    /** true si alguno de los roles puede hacer la transición. */
    public static boolean isAllowed(OrderStatusEnum from, OrderStatusEnum to, Collection<String> roles) {
        Set<String> allowed = TRANSITIONS.getOrDefault(from, Map.of()).get(to);
        return allowed != null && roles.stream().anyMatch(allowed::contains);
    }

    /** CAJERO solo cancela PENDIENTE; ADMIN cualquier pedido abierto; nadie cancela ENTREGADO/CANCELADO. */
    public static boolean canCancel(OrderStatusEnum status, Collection<String> roles) {
        if (!OPEN_STATUSES.contains(status)) {
            return false;
        }
        if (roles.contains(RoleCodes.ADMIN)) {
            return true;
        }
        return roles.contains(RoleCodes.CAJERO) && status == PENDING;
    }
}
