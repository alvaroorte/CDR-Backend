package bo.cdr.app.appcash.domain.entity;

import bo.cdr.app.appbase.domain.entity.Base;
import bo.cdr.app.appcatalog.domain.entity.PaymentMethod;
import bo.cdr.app.common.enums.PaymentStatusEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Pago de un pedido. {@code amount} es el importe aplicado al pedido: NO se guardan el importe
 * recibido ni el vuelto (decisión aprobada). {@code orderId} como columna simple para no acoplar
 * appcash con la entidad Order.
 */
@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(name = "payments")
public class Payment extends Base {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "order_id", nullable = false)
    private Integer orderId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cash_session_id", nullable = false)
    private CashSession cashSession;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_method_id", nullable = false)
    private PaymentMethod paymentMethod;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(length = 100)
    private String reference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PaymentStatusEnum status;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Payment other)) return false;
        return id != null && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
