package bo.cdr.app.appcash.domain.entity;

import bo.cdr.app.appbase.domain.entity.Base;
import bo.cdr.app.appcatalog.domain.entity.PaymentMethod;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Objects;

/** Arqueo de una caja cerrada, por medio de pago: esperado, declarado y diferencia (declarado - esperado). Inmutable. */
@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(name = "cash_session_summaries")
public class CashSessionSummary extends Base {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cash_session_id", nullable = false)
    private CashSession cashSession;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_method_id", nullable = false)
    private PaymentMethod paymentMethod;

    @Column(name = "expected_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal expectedAmount;

    @Column(name = "declared_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal declaredAmount;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal difference;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CashSessionSummary other)) return false;
        return id != null && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
