package bo.cdr.app.appsecurity.domain.entity;

import bo.cdr.app.appbase.domain.entity.Base;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.Hibernate;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "permission", schema = "security")
public class Permission extends Base {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column
    private String name;

    @Column
    private String description;

    private String httpMethod;

    @Column
    private String url;

    @Column
    private String urlWithoutIds;

    @Column(unique = true)
    private String code;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;

        if (Hibernate.getClass(this) != Hibernate.getClass(o)) return false;

        Permission permission = (Permission) o;
        return id != null && id.equals(permission.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
