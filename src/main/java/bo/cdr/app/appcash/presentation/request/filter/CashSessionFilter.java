package bo.cdr.app.appcash.presentation.request.filter;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/** Filtros del historial de cajas. Para el CAJERO, {@code userId} se fuerza al suyo en el servicio. */
@Data
@NoArgsConstructor
public class CashSessionFilter {

    private Integer userId;

    /** name() de CashSessionStatusEnum. */
    private String status;

    /** Fecha de apertura desde (inclusive). */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateFrom;

    /** Fecha de apertura hasta (inclusive). */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateTo;
}
