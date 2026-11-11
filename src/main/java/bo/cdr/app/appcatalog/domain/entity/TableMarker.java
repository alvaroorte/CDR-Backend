package bo.cdr.app.appcatalog.domain.entity;

import bo.cdr.app.appbase.domain.entity.Base;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Adorno numerado que el cajero entrega al cliente para ponerlo en su mesa.
 * La ocupación NO se guarda aquí: se deriva de los pedidos abiertos (Etapa 3).
 */
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Entity
@NoArgsConstructor
@Data
@Table(name = "table_markers")
public class TableMarker extends Base {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private Integer number;

    @Column(length = 255)
    private String description;
}
