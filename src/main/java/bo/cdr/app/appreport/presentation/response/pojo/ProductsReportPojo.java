package bo.cdr.app.appreport.presentation.response.pojo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Productos y categorías vendidos (pedidos no cancelados), con precio histórico de {@code order_items}.
 * {@code gross} es la suma de líneas <b>antes</b> del descuento general del pedido (el descuento no se
 * reparte por línea).
 */
public record ProductsReportPojo(LocalDate dateFrom,
                                 LocalDate dateTo,
                                 List<Product> products,
                                 List<Category> categories) {

    public record Product(Integer productId,
                          String productName,
                          String categoryName,
                          long quantity,
                          long orderCount,
                          BigDecimal gross,
                          BigDecimal sharePercent) {
    }

    public record Category(Integer categoryId,
                           String categoryName,
                           long quantity,
                           BigDecimal gross,
                           BigDecimal sharePercent) {
    }
}
