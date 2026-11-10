package bo.cdr.app.appsecurity.auth.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import bo.cdr.app.exception.TooManyLoginAttemptsException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Limita los intentos fallidos de login. Es el control anti-automatización de {@code POST /login}
 * que aplica a <b>todos</b> los clientes.
 *
 * <p>Lleva dos contadores independientes que se agotan por separado:
 * <ul>
 *   <li><b>por cuenta</b> — frena el ataque dirigido a un usuario desde muchas IPs;</li>
 *   <li><b>por IP</b> — frena el barrido de muchas cuentas desde un mismo origen.</li>
 * </ul>
 *
 * <p>Solo cuentan los intentos <b>fallidos</b>: un login correcto limpia ambos contadores, de modo
 * que un usuario que se equivoca y luego acierta no arrastra penalización.
 *
 * <p><b>El estado es en memoria</b> y se pierde en cada reinicio del pod. Hoy es suficiente porque
 * todos los entornos corren con {@code replicas: 1} (ver {@code k8s/overlays/*}); si alguna vez se
 * escala horizontalmente hay que mover los contadores a un almacén compartido (Redis), o cada
 * réplica permitirá el máximo por separado.
 */
@Slf4j
@Service
public class LoginRateLimiterService {

    private final boolean enabled;
    private final int maxAttempts;
    private final Duration window;
    private final Cache<String, AtomicInteger> attempts;

    // @Autowired explícito: hay dos constructores (este y el de tests), así que Spring no puede
    // elegir solo y buscaría uno sin argumentos.
    @Autowired
    public LoginRateLimiterService(
            @Value("${app.login.rate-limit.enabled:true}") boolean enabled,
            @Value("${app.login.rate-limit.max-attempts:10}") int maxAttempts,
            @Value("${app.login.rate-limit.window:15m}") Duration window) {
        this(enabled, maxAttempts, window, Ticker.systemTicker());
    }

    /** Constructor para tests: permite controlar el paso del tiempo sin dormir el hilo. */
    LoginRateLimiterService(boolean enabled, int maxAttempts, Duration window, Ticker ticker) {
        this.enabled = enabled;
        this.maxAttempts = maxAttempts;
        this.window = window;
        this.attempts = Caffeine.newBuilder()
                .expireAfterWrite(window)
                .ticker(ticker)
                .maximumSize(100_000)
                .build();
    }

    /**
     * Corta el login si la cuenta o la IP ya agotaron sus intentos.
     *
     * @throws TooManyLoginAttemptsException si se superó el máximo dentro de la ventana
     */
    public void checkNotBlocked(String username, String remoteIp) {
        if (!enabled) {
            return;
        }
        if (isBlocked(userKey(username)) || isBlocked(ipKey(remoteIp))) {
            log.warn("Login bloqueado por exceso de intentos. username={} ip={}", username, remoteIp);
            throw new TooManyLoginAttemptsException(
                    "Demasiados intentos fallidos. Espera unos minutos e intenta nuevamente.");
        }
    }

    /** Suma un intento fallido a la cuenta y a la IP. */
    public void recordFailure(String username, String remoteIp) {
        if (!enabled) {
            return;
        }
        increment(userKey(username));
        increment(ipKey(remoteIp));
    }

    /** Login correcto: limpia los contadores de la cuenta y de la IP. */
    public void reset(String username, String remoteIp) {
        if (!enabled) {
            return;
        }
        invalidate(userKey(username));
        invalidate(ipKey(remoteIp));
    }

    private void invalidate(String key) {
        // Caffeine lanza NPE con clave nula, y userKey/ipKey devuelven null si el dato falta.
        if (key != null) {
            attempts.invalidate(key);
        }
    }

    private boolean isBlocked(String key) {
        if (key == null) {
            return false;
        }
        AtomicInteger counter = attempts.getIfPresent(key);
        return counter != null && counter.get() >= maxAttempts;
    }

    private void increment(String key) {
        if (key == null) {
            return;
        }
        // Nota: `get` renueva expireAfterWrite solo al crear la entrada, así que la ventana se
        // cuenta desde el primer fallo y no se extiende indefinidamente con cada intento.
        attempts.get(key, k -> new AtomicInteger()).incrementAndGet();
    }

    private String userKey(String username) {
        return StringUtils.hasText(username) ? "user:" + username.toLowerCase(Locale.ROOT) : null;
    }

    private String ipKey(String remoteIp) {
        return StringUtils.hasText(remoteIp) ? "ip:" + remoteIp : null;
    }
}
