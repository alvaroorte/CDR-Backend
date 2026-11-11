package bo.cdr.app.appcatalog.domain.service.implement;

import bo.cdr.app.appcatalog.domain.event.ProductAvailabilityChangedEvent;
import bo.cdr.app.appcatalog.presentation.response.pojo.CatalogEventPojo;
import bo.cdr.app.util.constants.WebSocketTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

/** Agotado / disponible en vivo para los POS abiertos (/topic/catalog), solo tras el commit. */
@Slf4j
@Component
@RequiredArgsConstructor
public class CatalogEventPublisher {

    public static final String PRODUCT_AVAILABILITY_CHANGED = "PRODUCT_AVAILABILITY_CHANGED";

    private final SimpMessagingTemplate messagingTemplate;
    private final Clock clock;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAvailabilityChanged(ProductAvailabilityChangedEvent event) {
        try {
            messagingTemplate.convertAndSend(WebSocketTopics.CATALOG, new CatalogEventPojo(UUID.randomUUID().toString(),
                    PRODUCT_AVAILABILITY_CHANGED, event.productId(), event.available(), event.version(),
                    LocalDateTime.now(clock)));
        } catch (Exception e) {
            log.warn("No se pudo publicar la disponibilidad del producto {}: {}", event.productId(), e.getMessage());
        }
    }
}
