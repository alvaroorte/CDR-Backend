package bo.cdr.app.appsecurity.auth.service;

import com.github.benmanes.caffeine.cache.Ticker;
import bo.cdr.app.exception.TooManyLoginAttemptsException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoginRateLimiterServiceTest {

    private static final String EMAIL = "usuario@endesyc.bo";
    private static final String IP = "10.0.0.1";
    private static final Duration WINDOW = Duration.ofMinutes(15);

    /** Reloj manual: evita dormir el hilo para probar la expiración de la ventana. */
    private final AtomicLong nanos = new AtomicLong();
    private final Ticker ticker = nanos::get;

    private LoginRateLimiterService limiter(int maxAttempts) {
        return new LoginRateLimiterService(true, maxAttempts, WINDOW, ticker);
    }

    private void advance(Duration duration) {
        nanos.addAndGet(duration.toNanos());
    }

    @Test
    void allows_whileUnderTheLimit() {
        LoginRateLimiterService limiter = limiter(3);

        limiter.recordFailure(EMAIL, IP);
        limiter.recordFailure(EMAIL, IP);

        assertThatCode(() -> limiter.checkNotBlocked(EMAIL, IP)).doesNotThrowAnyException();
    }

    @Test
    void blocks_whenLimitReached() {
        LoginRateLimiterService limiter = limiter(3);

        for (int i = 0; i < 3; i++) {
            limiter.recordFailure(EMAIL, IP);
        }

        assertThatThrownBy(() -> limiter.checkNotBlocked(EMAIL, IP))
                .isInstanceOf(TooManyLoginAttemptsException.class)
                .hasMessageContaining("Demasiados intentos");
    }

    @Test
    void reset_clearsCountersOnSuccessfulLogin() {
        LoginRateLimiterService limiter = limiter(3);
        limiter.recordFailure(EMAIL, IP);
        limiter.recordFailure(EMAIL, IP);

        limiter.reset(EMAIL, IP);
        limiter.recordFailure(EMAIL, IP);

        assertThatCode(() -> limiter.checkNotBlocked(EMAIL, IP)).doesNotThrowAnyException();
    }

    @Test
    void unblocks_afterWindowExpires() {
        LoginRateLimiterService limiter = limiter(2);
        limiter.recordFailure(EMAIL, IP);
        limiter.recordFailure(EMAIL, IP);
        assertThatThrownBy(() -> limiter.checkNotBlocked(EMAIL, IP))
                .isInstanceOf(TooManyLoginAttemptsException.class);

        advance(WINDOW.plusSeconds(1));

        assertThatCode(() -> limiter.checkNotBlocked(EMAIL, IP)).doesNotThrowAnyException();
    }

    /** El contador por cuenta es independiente de la IP: cambiar de origen no lo evade. */
    @Test
    void blocksByAccount_evenFromDifferentIps() {
        LoginRateLimiterService limiter = limiter(2);

        limiter.recordFailure(EMAIL, "10.0.0.1");
        limiter.recordFailure(EMAIL, "10.0.0.2");

        assertThatThrownBy(() -> limiter.checkNotBlocked(EMAIL, "10.0.0.3"))
                .isInstanceOf(TooManyLoginAttemptsException.class);
    }

    /** El contador por IP frena el barrido de muchas cuentas desde un mismo origen. */
    @Test
    void blocksByIp_evenForDifferentAccounts() {
        LoginRateLimiterService limiter = limiter(2);

        limiter.recordFailure("uno@endesyc.bo", IP);
        limiter.recordFailure("dos@endesyc.bo", IP);

        assertThatThrownBy(() -> limiter.checkNotBlocked("tres@endesyc.bo", IP))
                .isInstanceOf(TooManyLoginAttemptsException.class);
    }

    @Test
    void treatsEmailCaseInsensitively() {
        LoginRateLimiterService limiter = limiter(2);

        limiter.recordFailure("Usuario@Endesyc.BO", "10.0.0.1");
        limiter.recordFailure("usuario@endesyc.bo", "10.0.0.2");

        assertThatThrownBy(() -> limiter.checkNotBlocked("USUARIO@ENDESYC.BO", "10.0.0.3"))
                .isInstanceOf(TooManyLoginAttemptsException.class);
    }

    @Test
    void isNoOp_whenDisabled() {
        LoginRateLimiterService limiter =
                new LoginRateLimiterService(false, 1, WINDOW, ticker);

        limiter.recordFailure(EMAIL, IP);
        limiter.recordFailure(EMAIL, IP);

        assertThatCode(() -> limiter.checkNotBlocked(EMAIL, IP)).doesNotThrowAnyException();
    }
}
