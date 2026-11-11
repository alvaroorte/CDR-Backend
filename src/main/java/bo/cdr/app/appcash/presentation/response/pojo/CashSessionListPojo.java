package bo.cdr.app.appcash.presentation.response.pojo;

import bo.cdr.app.common.enums.CashSessionStatusEnum;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Fila del historial de cajas. Totales del arqueo solo en cajas cerradas (null si está abierta). */
public record CashSessionListPojo(Integer id,
                                  Integer userId,
                                  String username,
                                  String userFullName,
                                  CashSessionStatusEnum status,
                                  String statusLabel,
                                  LocalDateTime openedAt,
                                  LocalDateTime closedAt,
                                  String closedBy,
                                  BigDecimal openingAmount,
                                  BigDecimal expectedTotal,
                                  BigDecimal declaredTotal,
                                  BigDecimal differenceTotal,
                                  Long version) {
}
