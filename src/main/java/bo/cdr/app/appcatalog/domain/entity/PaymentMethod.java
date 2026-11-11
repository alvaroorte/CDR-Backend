package bo.cdr.app.appcatalog.domain.entity;

import bo.cdr.app.appbase.domain.entity.Base;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/** Medio de pago configurable (EFECTIVO, QR...). */
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Entity
@NoArgsConstructor
@Data
@Table(name = "payment_methods")
public class PaymentMethod extends Base {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 30)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    /** Exige número de referencia al cobrar (QR, transferencia...). */
    @Column(name = "requires_reference", nullable = false)
    private Boolean requiresReference = false;

    /** El POS muestra el cálculo de vuelto (solo visual; no se guarda). */
    @Column(name = "gives_change", nullable = false)
    private Boolean givesChange = false;
}
