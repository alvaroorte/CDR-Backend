package bo.cdr.app.util;

/** Utilidades de imágenes en base64 (la referencia solo leía archivos a base64; aquí se normaliza la entrada). */
public final class ImageBase64Util {

    private ImageBase64Util() {}

    /** Quita el prefijo "data:image/...;base64," si viene del navegador; devuelve null si está vacío. */
    public static String stripDataUrlPrefix(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        int comma = value.indexOf(',');
        return value.startsWith("data:") && comma > 0 ? value.substring(comma + 1) : value.trim();
    }
}
