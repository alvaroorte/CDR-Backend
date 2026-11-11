package bo.cdr.app.appcatalog.domain.service.interfaces;

import bo.cdr.app.appcatalog.presentation.response.pojo.PosCatalogPojo;

public interface PosCatalogService {

    /** Categorías y productos activos (con imágenes y adicionales), medios de pago activos y reglas de descuento. */
    PosCatalogPojo getCatalog();
}
