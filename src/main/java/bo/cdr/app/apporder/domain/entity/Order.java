package bo.cdr.app.apporder.domain.entity;

import bo.cdr.app.appbase.domain.entity.Base;
import bo.cdr.app.appcash.domain.entity.CashSession;
import bo.cdr.app.appcatalog.domain.entity.TableMarker;
import bo.cdr.app.common.enums.DiscountTypeEnum;
import bo.cdr.app.common.enums.OrderStatusEnum;
import bo.cdr.app.common.enums.ServiceTypeEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Pedido. Los importes los calcula SOLO OrderPricingService; los nombres y precios del detalle
 * son fotografías (skill cdr-order-management §3). {@code code} es el correlativo del día
 * ({@code businessDate}, America/La_Paz). @Version: cocina y caja cambian el estado a la vez.
 * Nunca se borra; createdBy = usuario que lo registró.
 */
@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(name = "orders")
public class Order extends Base {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "business_date", nullable = false)
    private LocalDate businessDate;

    @Column(nullable = false)
    private Integer code;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_type", nullable = false, length = 10)
    private ServiceTypeEnum serviceType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "table_marker_id")
    private TableMarker tableMarker;

    /** Fotografía del número del adorno. */
    @Column(name = "table_marker_number")
    private Integer tableMarkerNumber;

    @Column(name = "customer_name", length = 100)
    private String customerName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatusEnum status;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", length = 15)
    private DiscountTypeEnum discountType;

    @Column(name = "discount_value", precision = 12, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Column(name = "discount_reason", length = 255)
    private String discountReason;

    @Column(name = "discount_applied_by", length = 150)
    private String discountAppliedBy;

    @Column(name = "discount_applied_at")
    private LocalDateTime discountAppliedAt;

    @Column(name = "cancel_reason", length = 255)
    private String cancelReason;

    @Column(name = "cancelled_by", length = 150)
    private String cancelledBy;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cash_session_id", nullable = false)
    private CashSession cashSession;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    @OrderBy("id")
    private List<OrderItem> items = new ArrayList<>();

    @Version
    private Long version;

    public void addItem(OrderItem item) {
        item.setOrder(this);
        items.add(item);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Order other)) return false;
        return id != null && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
