package bo.cdr.app.appreport.presentation.request.filter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * Filtros de reportes (@ModelAttribute). El rango de fechas de negocio es obligatorio; el resto es
 * opcional y se aplica donde tiene sentido (categoría solo en productos, medio de pago solo en pagos).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportFilter {

    /** Fecha de negocio desde (inclusive, America/La_Paz). Obligatoria. */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateFrom;

    /** Fecha de negocio hasta (inclusive). Obligatoria. */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateTo;

    /** name() de ReportGroupByEnum (solo ventas). Por defecto DAY. */
    private String groupBy;

    /** Cajero (dueño de la caja donde se registró el pedido). */
    private Integer userId;

    /** name() de ServiceTypeEnum. */
    private String serviceType;

    /** Solo reporte de productos. */
    private Integer categoryId;

    /** Solo reporte de medios de pago. */
    private Integer paymentMethodId;
}
