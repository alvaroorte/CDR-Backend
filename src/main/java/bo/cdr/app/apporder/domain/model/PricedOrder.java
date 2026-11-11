package bo.cdr.app.apporder.domain.model;

import bo.cdr.app.appcatalog.domain.entity.Addon;
import bo.cdr.app.appcatalog.domain.entity.Product;
import bo.cdr.app.common.enums.DiscountTypeEnum;

import java.math.BigDecimal;
import java.util.List;

/**
 * Resultado de OrderPricingService: importes ya redondeados (MoneyUtil) y entidades vigentes de
 * las que se toman las fotografías. Sin descuento: {@code discountType == null} y
 * {@code discountAmount == 0}.
 */
public record PricedOrder(List<Line> lines,
                          BigDecimal subtotal,
                          DiscountTypeEnum discountType,
                          BigDecimal discountValue,
                          BigDecimal discountAmount,
                          BigDecimal discountPercent,
                          String discountReason,
                          BigDecimal total) {

    public boolean hasDiscount() {
        return discountType != null;
    }

    public record Line(Product product,
                       String categoryName,
                       Integer categoryId,
                       int quantity,
                       BigDecimal unitPrice,
                       List<Addon> addons,
                       BigDecimal addonsUnitTotal,
                       BigDecimal lineSubtotal,
                       String notes) {
    }
}
