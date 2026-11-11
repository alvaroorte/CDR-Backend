package bo.cdr.app.appcatalog.presentation.response.pojo;

import java.math.BigDecimal;
import java.util.List;

/**
 * Todo lo que el POS necesita en una sola llamada (decisión aprobada: incluye las imágenes).
 * Solo categorías y productos activos; los productos agotados vienen con {@code available=false}
 * para mostrarse deshabilitados. Records sin @Pojo (esa anotación registra un @Component).
 */
public record PosCatalogPojo(List<Category> categories,
                             List<PaymentMethod> paymentMethods,
                             DiscountRules discountRules) {

    public record Category(Integer id, String name, String imageBase64, List<Product> products) {
    }

    public record Product(Integer id, Integer categoryId, String name, String description, String imageBase64,
                          BigDecimal price, boolean available, List<Addon> addons) {
    }

    public record Addon(Integer id, String name, BigDecimal price) {
    }

    public record PaymentMethod(Integer id, String code, String name, boolean requiresReference, boolean givesChange) {
    }

    /** Parámetros de descuento vigentes, para que el POS valide en vista previa (el backend valida igual). */
    public record DiscountRules(BigDecimal cashierMaxPercent, BigDecimal reasonRequiredAbovePercent) {
    }
}
