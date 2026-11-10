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
@Table(name = "role", schema = "security")
public class Role extends Base {

    @Id
    @GeneratedValue( strategy = GenerationType.IDENTITY )
    private Integer id;

    @Column( unique = true )
    private String name;

    @Column(length = 15, nullable = false)
    private String code;
    
    private String description;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;

        if (Hibernate.getClass(this) != Hibernate.getClass(o)) return false;

        Role role = (Role) o;
        return id != null && id.equals(role.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
