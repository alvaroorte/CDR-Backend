package bo.cdr.app.appreport.presentation.response.pojo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Ventas por cajero, pedidos por estado, descuentos aplicados y cancelaciones con motivo. */
public record OperationsReportPojo(LocalDate dateFrom,
                                   LocalDate dateTo,
                                   List<UserSales> users,
                                   List<StatusCount> statuses,
                                   List<DiscountOrder> discounts,
                                   List<CancelledOrder> cancellations) {

    /** Importes sin cancelados; {@code cancelledCount} cuenta los cancelados de sus pedidos. */
    public record UserSales(Integer userId,
                            String username,
                            String userFullName,
                            long orderCount,
                            BigDecimal gross,
                            BigDecimal discount,
                            BigDecimal net,
                            long cancelledCount) {
    }

    /** Incluye todos los estados (también CANCELADO), en el orden del flujo. */
    public record StatusCount(String status,
                              String statusLabel,
                              long orderCount,
                              BigDecimal total) {
    }

    public record DiscountOrder(Integer orderId,
                                String codeLabel,
                                LocalDate businessDate,
                                LocalDateTime createdAt,
                                String cashier,
                                String discountType,
                                String discountTypeLabel,
                                BigDecimal discountValue,
                                BigDecimal subtotal,
                                BigDecimal discountAmount,
                                BigDecimal total,
                                String discountReason,
                                String appliedBy) {
    }

    public record CancelledOrder(Integer orderId,
                                 String codeLabel,
                                 LocalDate businessDate,
                                 LocalDateTime createdAt,
                                 String cashier,
                                 BigDecimal total,
                                 String cancelReason,
                                 String cancelledBy,
                                 LocalDateTime cancelledAt) {
    }
}
