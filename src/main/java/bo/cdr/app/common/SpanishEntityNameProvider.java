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
    NAME_MAP.put("Category", "Categoría");
    NAME_MAP.put("Product", "Producto");
    NAME_MAP.put("Addon", "Adicional");
    NAME_MAP.put("TableMarker", "Número de mesa");
    NAME_MAP.put("PaymentMethod", "Medio de pago");
    NAME_MAP.put("Parameter", "Parámetro");
    NAME_MAP.put("CashSession", "Caja");
    NAME_MAP.put("CashMovement", "Movimiento de caja");
    NAME_MAP.put("CashSessionSummary", "Arqueo de caja");
    NAME_MAP.put("Payment", "Pago");
    NAME_MAP.put("Order", "Pedido");
    NAME_MAP.put("OrderItem", "Detalle del pedido");
    NAME_MAP.put("OrderItemAddon", "Adicional del detalle");
    NAME_MAP.put("OrderStatusHistory", "Historial del pedido");
  }

  private SpanishEntityNameProvider() {}

  public static String getSpanishName(String englishName) {
    return NAME_MAP.getOrDefault(englishName, englishName);
  }
}
