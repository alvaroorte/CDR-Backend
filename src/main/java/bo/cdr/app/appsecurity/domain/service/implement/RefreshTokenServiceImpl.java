package bo.cdr.app.appsecurity.domain.service.implement;

import bo.cdr.app.appsecurity.domain.service.interfaces.*;
import bo.cdr.app.appsecurity.domain.entity.RefreshToken;
import bo.cdr.app.appsecurity.domain.entity.UserApp;
import bo.cdr.app.appsecurity.data.RefreshTokenRepository;
import bo.cdr.app.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 64;

    @Value("${app.jwt.refresh-expiration:7d}")
    private Duration refreshExpiration;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String issue(UserApp user) {
        String rawToken = generateRawToken();

        RefreshToken entity = RefreshToken.builder()
                .username(user.getUsername())
                .tokenHash(hash(rawToken))
                .expiresAt(LocalDateTime.now().plus(refreshExpiration))
                .revoked(false)
                .build();

        refreshTokenRepository.save(entity);
        return rawToken;
    }

    @Override
    public RefreshToken validateUsable(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new UnauthorizedException("Refresh token inválido");
        }

        RefreshToken token = refreshTokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new UnauthorizedException("Refresh token inválido"));

        if (Boolean.TRUE.equals(token.getRevoked())) {
            throw new UnauthorizedException("Refresh token revocado");
        }

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new UnauthorizedException("Refresh token expirado");
        }

        return token;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revoke(RefreshToken token) {
        token.setRevoked(true);
        refreshTokenRepository.save(token);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revokeByRawToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByTokenHash(hash(rawToken))
                .ifPresent(this::revoke);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revokeAllForUser(String username) {
        refreshTokenRepository.findAllByUsernameAndRevokedFalse(username).forEach(this::revoke);
    }

    private String generateRawToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
