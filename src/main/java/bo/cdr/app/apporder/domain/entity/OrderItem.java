package bo.cdr.app.apporder.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Línea del pedido con fotografía de nombres y precios. {@code productId}/{@code categoryId}
 * son solo trazabilidad: reportes y detalle leen las columnas fotografiadas.
 */
@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "product_id", nullable = false)
    private Integer productId;

    @Column(name = "product_name", nullable = false, length = 100)
    private String productName;

    @Column(name = "category_id", nullable = false)
    private Integer categoryId;

    @Column(name = "category_name", nullable = false, length = 100)
    private String categoryName;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "addons_unit_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal addonsUnitTotal;

    @Column(name = "line_subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal lineSubtotal;

    @Column(length = 255)
    private String notes;

    @OneToMany(mappedBy = "orderItem", cascade = CascadeType.ALL)
    @OrderBy("id")
    private List<OrderItemAddon> addons = new ArrayList<>();

    public void addAddon(OrderItemAddon addon) {
        addon.setOrderItem(this);
        addons.add(addon);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrderItem other)) return false;
        return id != null && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
