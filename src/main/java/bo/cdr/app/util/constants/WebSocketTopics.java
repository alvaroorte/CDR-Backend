package bo.cdr.app.util.constants;

/** Destinos STOMP (docs/architecture/realtime.md §2). Deben coincidir con el frontend. */
public final class WebSocketTopics {

    public static final String ORDERS = "/topic/orders";
    public static final String CATALOG = "/topic/catalog";

    private WebSocketTopics() {}
}
