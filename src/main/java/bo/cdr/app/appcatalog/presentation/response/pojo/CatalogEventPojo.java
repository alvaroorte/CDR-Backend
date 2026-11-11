package bo.cdr.app.appcatalog.presentation.response.pojo;

import java.time.LocalDateTime;

/** Mensaje de /topic/catalog. Record sin @Pojo. */
public record CatalogEventPojo(String eventId, String type, Integer productId, boolean available, Long version,
                               LocalDateTime occurredAt) {
}
