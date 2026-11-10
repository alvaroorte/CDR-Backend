package bo.cdr.app.appsecurity.auth.jwt;

import bo.cdr.app.appsecurity.domain.entity.UserApp;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET =
            "76397924423F4528482B4D6251655468576D5A7134743777217A25432A46294A";

    private UserApp user() {
        UserApp u = new UserApp();
        u.setId(7);
        u.setUsername("juan.perez");
        u.setFullName("Juan Pérez");
        return u;
    }

    private String generate(JwtService service) {
        return service.generateToken(user(), List.of(new SimpleGrantedAuthority("CAJERO")));
    }

    private long expirationMillis(String token) {
        return Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET)))
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getExpiration()
                .getTime();
    }

    @Test
    void generateToken_usesConfiguredExpiration() {
        JwtService service = new JwtService(SECRET, Duration.ofMinutes(4));

        long before = System.currentTimeMillis();
        String token = generate(service);
        long after = System.currentTimeMillis();

        long fourMinutes = Duration.ofMinutes(4).toMillis();
        // El exp del JWT tiene resolución de segundos, de ahí el margen inferior.
        assertThat(expirationMillis(token)).isBetween(before + fourMinutes - 1000,
                                                     after + fourMinutes);
        assertThat(service.validateToken(token)).isTrue();
    }

    @Test
    void validateToken_returnsFalse_whenAlreadyExpired() {
        // Duración negativa: el token nace expirado.
        JwtService service = new JwtService(SECRET, Duration.ofMinutes(-1));

        assertThat(service.validateToken(generate(service))).isFalse();
    }

    @Test
    void generateToken_carriesUserClaims() {
        JwtService service = new JwtService(SECRET, Duration.ofMinutes(4));
        String token = generate(service);

        assertThat(service.extractUsername(token)).isEqualTo("juan.perez");
        assertThat(service.extractFullName(token)).isEqualTo("Juan Pérez");
        assertThat(service.extractUserId(token)).isEqualTo(7);
        assertThat(service.extractAuthorities(token)).containsExactly("CAJERO");
    }

    @Test
    void validateToken_returnsFalse_whenSignedWithAnotherSecret() {
        JwtService issuer = new JwtService(SECRET, Duration.ofMinutes(4));
        JwtService other = new JwtService(
                "2A46294A76397924423F4528482B4D6251655468576D5A7134743777217A2543",
                Duration.ofMinutes(4));

        assertThat(other.validateToken(generate(issuer))).isFalse();
    }
}
