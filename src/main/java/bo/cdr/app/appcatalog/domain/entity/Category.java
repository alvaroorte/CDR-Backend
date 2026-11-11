package bo.cdr.app.appcatalog.domain.entity;

import bo.cdr.app.appbase.domain.entity.Base;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(name = "categories")
public class Category extends Base {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "image_base64", columnDefinition = "TEXT")
    @JsonIgnore
    private String imageBase64;

    /** Orden de aparición en el POS (menor primero). */
    @Column(nullable = false)
    private Integer ranking = 0;

    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Product> products = new ArrayList<>();

    /** Patrón hasDependencies de la referencia, usado por CRUDServiceImpl.checkIfElementsExist. */
    public Map<String, Boolean> hasDependencies() {
        return Map.of("productos activos", products.stream().anyMatch(p -> Boolean.TRUE.equals(p.getActive())));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Category other)) return false;
        return id != null && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
