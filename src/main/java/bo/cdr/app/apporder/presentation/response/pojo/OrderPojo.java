package bo.cdr.app.apporder.presentation.response.pojo;

import bo.cdr.app.appcash.presentation.response.pojo.PaymentPojo;
import bo.cdr.app.common.Pojo;
import bo.cdr.app.common.enums.DiscountTypeEnum;
import bo.cdr.app.common.enums.OrderStatusEnum;
import bo.cdr.app.common.enums.ServiceTypeEnum;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Detalle de un pedido registrado (leído de las fotografías, nunca del catálogo). */
@Pojo
@Data
@NoArgsConstructor
public class OrderPojo {
    private Integer id;
    private Integer code;
    /** Código visible: "#003". */
    private String codeLabel;
    private LocalDate businessDate;
    private ServiceTypeEnum serviceType;
    private String serviceTypeLabel;
    private Integer tableMarkerId;
    private Integer tableMarkerNumber;
    private String customerName;
    private OrderStatusEnum status;
    private String statusLabel;
    private BigDecimal subtotal;
    private DiscountTypeEnum discountType;
    private BigDecimal discountValue;
    private BigDecimal discountAmount;
    private String discountReason;
    private String discountAppliedBy;
    private BigDecimal total;
    private Integer cashSessionId;
    private List<OrderItemPojo> items = new ArrayList<>();
    private List<PaymentPojo> payments = new ArrayList<>();
    private List<OrderStatusHistoryPojo> statusHistory = new ArrayList<>();
    private String cancelReason;
    private String cancelledBy;
    private LocalDateTime cancelledAt;
    private String createdBy;
    private LocalDateTime createdAt;
    private Long version;
}
