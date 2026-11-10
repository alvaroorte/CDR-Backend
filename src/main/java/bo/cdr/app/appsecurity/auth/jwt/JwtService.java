package bo.cdr.app.appsecurity.auth.jwt;

import bo.cdr.app.appsecurity.auth.service.CustomUserDetails;
import bo.cdr.app.appsecurity.domain.entity.UserApp;
import bo.cdr.app.exception.UnauthorizedException;
import bo.cdr.app.util.constants.RoleCodes;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * Emisión y validación de JWT (JJWT, HS256), copiado de la referencia.
 * Cambios: claim {@code fullName} en lugar de {@code userEmail}; {@code isAdmin} por rol ADMIN.
 */
@Service
public class JwtService {

    public static final String CLAIM_AUTHORITIES = "authorities";
    public static final String CLAIM_IS_ADMIN = "isAdmin";
    public static final String CLAIM_USER_ID = "userId";
    public static final String CLAIM_FULL_NAME = "fullName";

    private final SecretKey secretKey;
    private final Duration expiration;

    public JwtService(@Value("${app.jwt.secret}") String jwtSecret,
                      @Value("${app.jwt.expiration:12h}") Duration expiration) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
        this.expiration = expiration;
    }

    // ==============================
    // Extract token from request / STOMP header
    // ==============================
    public String extractToken(HttpServletRequest request) {
        return extractBearer(request.getHeader("Authorization"));
    }

    public String extractBearer(String header) {
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    // ==============================
    // Extract data from token
    // ==============================
    public String extractUsername(String token) {
        return getClaims(token).getSubject();
    }

    public String extractFullName(String token) {
        return getClaims(token).get(CLAIM_FULL_NAME, String.class);
    }

    public Integer extractUserId(String token) {
        return getClaims(token).get(CLAIM_USER_ID, Integer.class);
    }

    @SuppressWarnings("unchecked")
    public List<String> extractAuthorities(String token) {
        return getClaims(token).get(CLAIM_AUTHORITIES, List.class);
    }

    // ==============================
    // Token validation
    // ==============================
    public boolean validateToken(String token) {
        try {
            getClaims(token); // valida firma + formato
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    // ==============================
    // Token generation
    // ==============================
    public String generateToken(UserApp user, Collection<? extends GrantedAuthority> authorities) {

        List<String> roles = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .distinct()
                .toList();

        Date now = new Date();
        Date expiresAt = new Date(now.getTime() + expiration.toMillis());

        return Jwts.builder()
                .subject(user.getUsername())
                .issuedAt(now)
                .expiration(expiresAt)
                .claim(CLAIM_AUTHORITIES, roles)
                .claim(CLAIM_IS_ADMIN, roles.contains(RoleCodes.ADMIN))
                .claim(CLAIM_USER_ID, user.getId())
                .claim(CLAIM_FULL_NAME, user.getFullName())
                .signWith(secretKey)
                .compact();
    }

    // ==============================
    // Internal methods
    // ==============================
    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private boolean isTokenExpired(String token) {
        return getClaims(token).getExpiration().before(new Date());
    }

    // ==============================
    // Current user (SecurityContext)
    // ==============================
    public Authentication getAuthentication() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new UnauthorizedException("Usuario no autenticado");
        }
        return auth;
    }

    public CustomUserDetails getCurrentUser() {
        return (CustomUserDetails) getAuthentication().getPrincipal();
    }

    public String getCurrentUsername() {
        return getCurrentUser().getUsername();
    }

    public Integer getCurrentUserId() {
        return getCurrentUser().getUserId();
    }

    public List<String> getCurrentAuthorities() {
        return getCurrentUser().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
    }

    public boolean currentUserHasRole(String roleCode) {
        return getCurrentAuthorities().contains(roleCode);
    }
}
