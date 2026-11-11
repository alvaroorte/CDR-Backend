package bo.cdr.app.appcatalog.domain.entity;

import bo.cdr.app.appbase.domain.entity.Base;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** Adicional global (p. ej. "Leche de almendra +3 Bs"), asignable a varios productos. */
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Entity
@NoArgsConstructor
@Data
@Table(name = "addons")
public class Addon extends Base {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;
}
