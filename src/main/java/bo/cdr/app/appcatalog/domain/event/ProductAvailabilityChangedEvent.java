package bo.cdr.app.appcatalog.domain.event;

/** Agotado / disponible: se publica en /topic/catalog tras el commit (CatalogEventPublisher). */
public record ProductAvailabilityChangedEvent(Integer productId, boolean available, Long version) {
}
