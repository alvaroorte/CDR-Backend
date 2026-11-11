package bo.cdr.app.appcatalog.domain.entity;

import bo.cdr.app.appbase.domain.entity.Base;
import bo.cdr.app.common.enums.ParameterFolderEnum;
import bo.cdr.app.common.enums.ParameterValueTypeEnum;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Parámetro del sistema (patrón Parameter de la referencia: carpeta + valor) con {@code code} único
 * para leerlo desde el código (p. ej. CASHIER_MAX_DISCOUNT_PERCENT) y {@code valueType} para validarlo.
 * Los parámetros los define una migración; desde la UI solo se edita el valor.
 */
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Entity
@NoArgsConstructor
@Data
@Table(name = "parameters")
public class Parameter extends Base {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ParameterFolderEnum folder;

    @Column(nullable = false, length = 60)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 255)
    private String value;

    @Enumerated(EnumType.STRING)
    @Column(name = "value_type", nullable = false, length = 20)
    private ParameterValueTypeEnum valueType;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private Boolean editable = true;
}
