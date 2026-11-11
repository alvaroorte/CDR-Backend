package bo.cdr.app.appcash.domain.entity;

import bo.cdr.app.appbase.domain.entity.Base;
import bo.cdr.app.appsecurity.domain.entity.UserApp;
import bo.cdr.app.common.enums.CashSessionStatusEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Sesión de caja de un usuario (una OPEN por usuario: índice ux_cash_sessions_user_open).
 * Todo pago queda ligado a la sesión abierta de quien cobra. Nunca se borra.
 */
@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(name = "cash_sessions")
public class CashSession extends Base {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserApp user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private CashSessionStatusEnum status;

    @Column(name = "opening_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal openingAmount;

    @Column(name = "opened_at", nullable = false)
    private LocalDateTime openedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "closing_note", length = 500)
    private String closingNote;

    @Version
    private Long version;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CashSession other)) return false;
        return id != null && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
