package bo.cdr.app.appcatalog.presentation.response.pojo;

/** Fila producto ↔ adicional para armar la lista de adicionales de cada producto en una sola consulta. */
public record ProductAddonProjection(Integer productId, Integer addonId, String addonName) {
}
