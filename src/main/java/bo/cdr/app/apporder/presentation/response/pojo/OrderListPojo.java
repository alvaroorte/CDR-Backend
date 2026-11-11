package bo.cdr.app.apporder.presentation.response.pojo;

import bo.cdr.app.common.enums.OrderStatusEnum;
import bo.cdr.app.common.enums.ServiceTypeEnum;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Fila del historial de pedidos. Record sin @Pojo. */
public record OrderListPojo(Integer id,
                            String codeLabel,
                            LocalDate businessDate,
                            LocalDateTime createdAt,
                            ServiceTypeEnum serviceType,
                            String serviceTypeLabel,
                            Integer tableMarkerNumber,
                            String customerName,
                            OrderStatusEnum status,
                            String statusLabel,
                            BigDecimal subtotal,
                            BigDecimal discountAmount,
                            BigDecimal total,
                            String createdBy,
                            Long version) {
}
