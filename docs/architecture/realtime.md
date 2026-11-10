# Tiempo real (cdr-backend + cdr-frontend)

**Reutiliza la tecnología de la referencia**: STOMP sobre WebSocket con fallback SockJS, broker simple
en memoria, `SimpMessagingTemplate` para enviar (`../ticket-backend/.../config/WebSocketConfig.java`,
`../ticket-frontend/.../core/services/message.service.ts`). Se añade autenticación, autorización,
eventos con versión y resincronización.

## 1. Servidor

```java
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Value("${app.cors.allowed-origins}") private String allowedOrigins;
    private final WebSocketAuthInterceptor webSocketAuthInterceptor;

    @Override public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }
    @Override public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-cdr").setAllowedOriginPatterns(allowedOrigins).withSockJS()
                .setTransportHandlers(new WebSocketTransportHandler(new DefaultHandshakeHandler()),
                        new XhrStreamingTransportHandler(), new XhrPollingTransportHandler(),
                        new XhrReceivingTransportHandler());
    }
    @Override public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(webSocketAuthInterceptor);   // NUEVO vs referencia
    }
}
```

Autenticación/autorización del canal: ver `security.md` §5.

## 2. Destinos

| Topic | Suscriptores | Eventos |
|---|---|---|
| `/topic/orders` | ADMIN, CAJERO, COCINA | `ORDER_CREATED`, `ORDER_STATUS_CHANGED`, `ORDER_CANCELLED` |
| `/topic/catalog` | ADMIN, CAJERO | `PRODUCT_AVAILABILITY_CHANGED` |

Un solo topic por dominio (no por usuario): el volumen de una cafetería es bajo y todos los roles
operativos ven el tablero. Cada cliente filtra lo que muestra (cocina: PENDIENTE/EN_PREPARACION/LISTO).

## 3. Contrato del evento

```json
{
  "eventId": "6f1c...-uuid",
  "type": "ORDER_STATUS_CHANGED",
  "orderId": 152,
  "version": 3,
  "status": "LISTO",
  "occurredAt": "2026-11-10T10:42:13",
  "order": { /* OrderBoardPojo: id, code, serviceType, tableMarkerNumber, customerName,
               status, version, createdAt, createdBy, items[{productName, quantity, notes, addons[]}] */ }
}
```

`OrderEventPojo` en `apporder/presentation/response/pojo`. `version` = `Order.version` tras el commit.

## 4. Publicación (siempre después del commit)

```java
// en el servicio, dentro de la transacción
applicationEventPublisher.publishEvent(new OrderChangedEvent(order.getId(), OrderEventType.ORDER_CREATED));

// OrderEventPublisher
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void on(OrderChangedEvent e) {
    OrderBoardPojo pojo = orderRepository.getBoardPojo(e.orderId());   // estado ya confirmado
    messagingTemplate.convertAndSend("/topic/orders", OrderEventPojo.of(e, pojo));
}
```

Así nunca se emite un evento de algo que hizo rollback.

## 5. Cliente: sincronización, reconexión y duplicados

1. Tras login, `OrderWsService.connect()` crea el `Client` de `@stomp/stompjs` con
   `webSocketFactory: () => new SockJS(server_url + 'ws-cdr')`, `reconnectDelay: 3000` (como la
   referencia) y `beforeConnect` que pone `connectHeaders = { Authorization: 'Bearer ' + token }`
   (token **actual**, ya refrescado).
2. `onConnect` → suscribe `/topic/orders` → **luego** llama `GET /order/board?statuses=...` y reemplaza el estado local
   (resincronización inicial y en **cada** reconexión; cubre eventos perdidos durante la caída).
3. Estado local: `Map<orderId, OrderBoard>`; al llegar un evento:
   - `event.version <= local.version` → **ignorar** (duplicado o desordenado).
   - si no → reemplazar `order` y su `version`; `CANCELLED`/`DELIVERED` se retiran del tablero de cocina.
4. `onWebSocketClose` / `onStompError` → signal `connected=false` → banner "Sin conexión — reconectando...".
   Mientras está desconectado, los botones de cambio de estado siguen funcionando por REST
   (el 409 por versión protege la consistencia).
5. Al hacer logout → `deactivate()`.

## 6. Escalabilidad (documentado, no implementado)

El broker simple en memoria **no se comparte entre réplicas** (misma limitación que la referencia,
que corre 2 réplicas en prod). Para la cafetería se despliega **una réplica** del backend. Si en el
futuro se necesitan varias: `enableStompBrokerRelay` (RabbitMQ) — requiere aprobación.
