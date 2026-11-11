package bo.cdr.app.appcatalog.presentation.response.pojo;

import java.math.BigDecimal;

/** Adicional activo asignado a un producto, con precio (catálogo del POS). */
public record PosProductAddonProjection(Integer productId, Integer addonId, String addonName, BigDecimal price) {
}
