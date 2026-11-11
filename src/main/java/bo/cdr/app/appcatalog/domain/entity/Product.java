package bo.cdr.app.appcatalog.domain.entity;

import bo.cdr.app.appbase.domain.entity.Base;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Producto del catálogo. {@code available=false} = agotado temporalmente (sigue en el catálogo);
 * {@code active=false} = dado de baja. @Version: el cajero cambia la disponibilidad mientras el
 * ADMIN puede estar editando el producto.
 */
@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(name = "products")
public class Product extends Base {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    @JsonIgnore
    private Category category;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "image_base64", columnDefinition = "TEXT")
    @JsonIgnore
    private String imageBase64;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private Boolean available = true;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "product_addons",
            joinColumns = @JoinColumn(name = "product_id"),
            inverseJoinColumns = @JoinColumn(name = "addon_id"))
    @JsonIgnore
    private Set<Addon> addons = new HashSet<>();

    @Version
    private Long version;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Product other)) return false;
        return id != null && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
