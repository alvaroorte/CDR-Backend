package bo.cdr.app.appsecurity.domain.entity;

import bo.cdr.app.appbase.domain.entity.Base;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.Hibernate;

/**
 * Usuario del sistema. A diferencia de la referencia (login por email, usuarios LDAP), el personal
 * de la cafetería inicia sesión con {@code username}; el email es opcional.
 */
@Getter
@Setter
@ToString(exclude = "password")
@NoArgsConstructor
@Entity
@Table(name = "user_app", schema = "security")
public class UserApp extends Base {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(length = 150)
    private String email;

    @JsonIgnore
    @Column(nullable = false, length = 100)
    private String password;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        UserApp user = (UserApp) o;
        return id != null && id.equals(user.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
