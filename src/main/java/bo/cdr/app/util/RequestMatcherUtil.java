package bo.cdr.app.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.AntPathMatcher;

import java.util.Arrays;

/**
 * Listas estáticas de rutas (patrón de la referencia). Editar esta clase al agregar rutas públicas
 * o rutas que solo requieren estar autenticado.
 */
public class RequestMatcherUtil {

  private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

  /** Sin token. */
  public static final String[] TOKEN_NOT_REQUIRED = {
          "/v3/api-docs/**",
          "/swagger-ui/**",
          "/swagger-ui.html",
          "/login",
          "/auth/refresh",
          "/auth/logout",
          "/error",
          // Handshake público; la autenticación ocurre en el frame STOMP CONNECT (Etapa 4).
          "/ws-cdr/**"
  };

  /** Con token pero sin comprobación de permiso por URL. */
  public static final String[] PERMISSION_NOT_REQUIRED = {
          "/users/me"
  };

  private RequestMatcherUtil() {}

  public static boolean isPermissionNotRequired(HttpServletRequest request) {
    String path = request.getServletPath();
    return Arrays.stream(PERMISSION_NOT_REQUIRED).anyMatch(p -> PATH_MATCHER.match(p, path));
  }

  public static boolean isTokenNotRequired(HttpServletRequest request) {
    String path = request.getServletPath();
    return Arrays.stream(TOKEN_NOT_REQUIRED).anyMatch(p -> PATH_MATCHER.match(p, path));
  }
}
