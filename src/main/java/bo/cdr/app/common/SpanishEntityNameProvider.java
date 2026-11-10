package bo.cdr.app.common;

import java.util.HashMap;
import java.util.Map;

/**
 * Traduce el nombre simple de la entidad a español para los mensajes de error
 * (p. ej. "Usuario con ID 9 no encontrado."). Al crear una entidad nueva, registrarla aquí.
 */
public class SpanishEntityNameProvider {

  private static final Map<String, String> NAME_MAP = new HashMap<>();

  static {
    NAME_MAP.put("UserApp", "Usuario");
    NAME_MAP.put("Role", "Rol");
    NAME_MAP.put("Permission", "Permiso");
    NAME_MAP.put("UserRole", "Rol de usuario");
    NAME_MAP.put("RolePermission", "Permiso de rol");
    NAME_MAP.put("RefreshToken", "Token de renovación");
  }

  private SpanishEntityNameProvider() {}

  public static String getSpanishName(String englishName) {
    return NAME_MAP.getOrDefault(englishName, englishName);
  }
}
