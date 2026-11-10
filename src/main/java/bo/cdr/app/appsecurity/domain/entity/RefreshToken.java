package bo.cdr.app.appsecurity.domain.entity;

import bo.cdr.app.appbase.domain.entity.Base;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "refresh_token", schema = "security")
public class RefreshToken extends Base {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** Username del UserApp (identificador de login). La referencia usaba el email. */
    @Column(nullable = false, length = 50)
    private String username;

    /** Hash SHA-256 del token opaco (nunca se guarda el valor en claro). */
    @Column(name = "token_hash", nullable = false, unique = true, length = 100)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Builder.Default
    @Column(nullable = false)
    private Boolean revoked = false;
}
