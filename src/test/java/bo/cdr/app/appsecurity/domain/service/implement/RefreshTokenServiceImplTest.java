package bo.cdr.app.appsecurity.domain.service.implement;
import bo.cdr.app.appsecurity.domain.service.interfaces.*;

import bo.cdr.app.appsecurity.domain.entity.RefreshToken;
import bo.cdr.app.appsecurity.domain.entity.UserApp;
import bo.cdr.app.appsecurity.data.RefreshTokenRepository;
import bo.cdr.app.exception.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceImplTest {

    @Mock
    RefreshTokenRepository repository;

    @InjectMocks
    RefreshTokenServiceImpl service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "refreshExpiration", Duration.ofDays(30));
    }

    private UserApp user() {
        UserApp u = new UserApp();
        u.setId(1);
        u.setUsername("juan.perez");
        return u;
    }

    @Test
    void issue_persistsHashedToken_andReturnsRawToken() {
        when(repository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

        String raw = service.issue(user());

        assertThat(raw).isNotBlank();

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository).save(captor.capture());
        RefreshToken saved = captor.getValue();

        assertThat(saved.getUsername()).isEqualTo("juan.perez");
        assertThat(saved.getRevoked()).isFalse();
        assertThat(saved.getExpiresAt()).isAfter(LocalDateTime.now());
        // Nunca se guarda el token en claro.
        assertThat(saved.getTokenHash()).isNotEqualTo(raw);
    }

    @Test
    void issue_usesConfiguredExpiration() {
        ReflectionTestUtils.setField(service, "refreshExpiration", Duration.ofHours(1));
        when(repository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

        LocalDateTime before = LocalDateTime.now();
        service.issue(user());

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository).save(captor.capture());

        assertThat(captor.getValue().getExpiresAt())
                .isBetween(before.plusHours(1), LocalDateTime.now().plusHours(1));
    }

    @Test
    void validateUsable_returnsToken_whenValid() {
        RefreshToken token = RefreshToken.builder()
                .username("juan.perez")
                .revoked(false)
                .expiresAt(LocalDateTime.now().plusDays(1))
                .build();
        when(repository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        assertThat(service.validateUsable("raw")).isSameAs(token);
    }

    @Test
    void validateUsable_throws_whenBlank() {
        assertThatThrownBy(() -> service.validateUsable("  "))
                .isInstanceOf(UnauthorizedException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void validateUsable_throws_whenNotFound() {
        when(repository.findByTokenHash(anyString())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.validateUsable("raw"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void validateUsable_throws_whenRevoked() {
        RefreshToken token = RefreshToken.builder()
                .revoked(true)
                .expiresAt(LocalDateTime.now().plusDays(1))
                .build();
        when(repository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.validateUsable("raw"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("revocado");
    }

    @Test
    void validateUsable_throws_whenExpired() {
        RefreshToken token = RefreshToken.builder()
                .revoked(false)
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .build();
        when(repository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.validateUsable("raw"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("expirado");
    }

    @Test
    void revoke_setsFlagAndSaves() {
        RefreshToken token = RefreshToken.builder().revoked(false).build();
        service.revoke(token);
        assertThat(token.getRevoked()).isTrue();
        verify(repository).save(token);
    }

    @Test
    void revokeByRawToken_revokesWhenFound() {
        RefreshToken token = RefreshToken.builder().revoked(false).build();
        when(repository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        service.revokeByRawToken("raw");

        assertThat(token.getRevoked()).isTrue();
        verify(repository).save(token);
    }

    @Test
    void revokeByRawToken_noOp_whenBlank() {
        service.revokeByRawToken("");
        verifyNoInteractions(repository);
    }
}
