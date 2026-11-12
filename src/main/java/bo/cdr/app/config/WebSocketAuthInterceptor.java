package bo.cdr.app.config;

import bo.cdr.app.appsecurity.auth.jwt.JwtAuthenticationFilter;
import bo.cdr.app.appsecurity.auth.jwt.JwtService;
import bo.cdr.app.util.constants.RoleCodes;
import bo.cdr.app.util.constants.WebSocketTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Map;
import java.util.Set;

/**
 * Seguridad del canal STOMP (desviación aprobada: la referencia no autenticaba):
 * CONNECT exige un JWT válido en el header nativo {@code Authorization: Bearer ...};
 * SUBSCRIBE solo a topics conocidos y por rol; SEND a /app no se usa en v1 → rechazado.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    /** Topic → roles que pueden suscribirse. */
    static final Map<String, Set<String>> TOPIC_ROLES = Map.of(
            WebSocketTopics.ORDERS, Set.of(RoleCodes.ADMIN, RoleCodes.CAJERO, RoleCodes.COCINA),
            WebSocketTopics.CATALOG, Set.of(RoleCodes.ADMIN, RoleCodes.CAJERO));

    private final JwtService jwtService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }
        switch (accessor.getCommand()) {
            case CONNECT -> authenticate(accessor);
            case SUBSCRIBE -> authorizeSubscription(accessor);
            case SEND -> throw new AccessDeniedException("El envío de mensajes por WebSocket no está habilitado.");
            default -> { /* DISCONNECT, UNSUBSCRIBE, ACK...: sin control */ }
        }
        return message;
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String token = jwtService.extractBearer(accessor.getFirstNativeHeader("Authorization"));
        if (token == null || !jwtService.validateToken(token)) {
            throw new AccessDeniedException("Token inválido o ausente en la conexión WebSocket.");
        }
        Authentication authentication = jwtAuthenticationFilter.buildAuthentication(token);
        if (authentication == null) {
            throw new AccessDeniedException("Usuario inactivo o sin roles vigentes.");
        }
        accessor.setUser(authentication);
    }

    private void authorizeSubscription(StompHeaderAccessor accessor) {
        Principal user = accessor.getUser();
        if (!(user instanceof Authentication authentication)) {
            throw new AccessDeniedException("Conexión WebSocket no autenticada.");
        }
        Set<String> allowed = TOPIC_ROLES.get(accessor.getDestination());
        boolean permitted = allowed != null && authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(allowed::contains);
        if (!permitted) {
            log.warn("Suscripción rechazada: {} a {}", authentication.getName(), accessor.getDestination());
            throw new AccessDeniedException("No tiene permiso para suscribirse a " + accessor.getDestination());
        }
    }
}
