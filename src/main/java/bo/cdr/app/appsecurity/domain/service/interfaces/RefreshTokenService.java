package bo.cdr.app.appsecurity.domain.service.interfaces;

import bo.cdr.app.appsecurity.domain.entity.RefreshToken;
import bo.cdr.app.appsecurity.domain.entity.UserApp;

public interface RefreshTokenService {

    /** Genera un refresh token opaco, persiste su hash y devuelve el valor en claro (solo aquí). */
    String issue(UserApp user);

    /** Valida que el token exista, no esté revocado ni expirado. Lanza UnauthorizedException si no. */
    RefreshToken validateUsable(String rawToken);

    /** Marca un refresh token como revocado. */
    void revoke(RefreshToken token);

    /** Revoca el refresh token cuyo valor en claro se recibe (idempotente). */
    void revokeByRawToken(String rawToken);

    /** Revoca todas las sesiones del usuario (al desactivarlo o restablecer su contraseña). */
    void revokeAllForUser(String username);
}
