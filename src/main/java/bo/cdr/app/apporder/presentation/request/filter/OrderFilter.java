package bo.cdr.app.apporder.presentation.request.filter;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/** Filtros del historial de pedidos (@ModelAttribute, patrón de la referencia). Todos opcionales. */
@Data
@NoArgsConstructor
public class OrderFilter {

    /** Fecha de negocio desde (inclusive). */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateFrom;

    /** Fecha de negocio hasta (inclusive). */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateTo;

    /** name() de OrderStatusEnum. */
    private String status;

    /** name() de ServiceTypeEnum. */
    private String serviceType;

    /** Código del día (#003 → 3). */
    private Integer code;
}
