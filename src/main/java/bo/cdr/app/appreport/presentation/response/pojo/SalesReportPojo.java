package bo.cdr.app.appreport.presentation.response.pojo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Ventas del rango: resumen (Bruto / Descuentos / Neto / Pagado), serie por período y por modalidad.
 * Excluye cancelados salvo en {@code cancelledCount} / {@code cancelledAmount}.
 */
public record SalesReportPojo(LocalDate dateFrom,
                              LocalDate dateTo,
                              String groupBy,
                              String groupByLabel,
                              Summary summary,
                              List<Period> periods,
                              List<ServiceType> serviceTypes) {

    /** {@code paid} = pagos PAID; {@code refunded} = devoluciones de pedidos cancelados del rango. */
    public record Summary(long orderCount,
                          BigDecimal gross,
                          BigDecimal discount,
                          BigDecimal net,
                          BigDecimal paid,
                          BigDecimal refunded,
                          BigDecimal averageTicket,
                          long cancelledCount,
                          BigDecimal cancelledAmount) {
    }

    public record Period(LocalDate periodStart,
                         long orderCount,
                         BigDecimal gross,
                         BigDecimal discount,
                         BigDecimal net) {
    }

    public record ServiceType(String serviceType,
                              String serviceTypeLabel,
                              long orderCount,
                              BigDecimal net,
                              BigDecimal sharePercent) {
    }
}
