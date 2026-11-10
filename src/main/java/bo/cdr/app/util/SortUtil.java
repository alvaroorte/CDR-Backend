package bo.cdr.app.util;

import bo.cdr.app.exception.InvalidValueException;
import org.springframework.data.domain.Sort;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SortUtil {

  private SortUtil() {}

  /** Copiado de la referencia: varios campos separados por coma con la misma dirección. */
  public static Sort createSort(String sortOrder, String fields) {
    List<String> fieldList = Arrays.asList(fields.split(",\\s*"));
    List<Sort.Order> orders = fieldList.stream()
        .map(field -> new Sort.Order(Sort.Direction.fromString(sortOrder), field))
        .collect(Collectors.toList());
    return Sort.by(orders);
  }

  /**
   * Ordenamiento con lista blanca de campos (patrón de UserServiceImpl de la referencia): evita que
   * el cliente ordene por propiedades arbitrarias. Dirección por defecto: DESC.
   */
  public static Sort build(String sortBy, String sortOrder, Set<String> allowedFields) {
    String field = (sortBy == null || sortBy.isBlank()) ? "id" : sortBy;
    if (!allowedFields.contains(field)) {
      throw new InvalidValueException("sortBy", field);
    }
    Sort.Direction direction = Sort.Direction.fromOptionalString(sortOrder).orElse(Sort.Direction.DESC);
    return Sort.by(direction, field);
  }
}
