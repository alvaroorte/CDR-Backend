package bo.cdr.app.appcash.domain.entity;

import bo.cdr.app.appbase.domain.entity.Base;
import bo.cdr.app.appcatalog.domain.entity.PaymentMethod;
import bo.cdr.app.common.enums.CashMovementTypeEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Bitácora inmutable de caja: nunca se actualiza ni se borra. {@code amount} siempre positivo;
 * el signo lo da el tipo (REFUND/EXPENSE restan). {@code paymentMethod} solo en SALE/REFUND;
 * OPENING/INCOME/EXPENSE son efectivo.
 */
@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(name = "cash_movements")
public class CashMovement extends Base {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cash_session_id", nullable = false)
    private CashSession cashSession;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private CashMovementTypeEnum type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_method_id")
    private PaymentMethod paymentMethod;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "order_id")
    private Integer orderId;

    @Column(name = "payment_id")
    private Integer paymentId;

    @Column(length = 255)
    private String description;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CashMovement other)) return false;
        return id != null && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
