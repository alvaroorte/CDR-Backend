package bo.cdr.app.appcash.presentation.response.pojo;

import bo.cdr.app.common.Pojo;
import bo.cdr.app.common.enums.CashSessionStatusEnum;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Caja abierta del usuario con sus totales en curso. {@code expectedCash} es informativo
 * (apertura + ventas en efectivo − devoluciones en efectivo + ingresos − egresos); el arqueo
 * y el cierre son de la Etapa 5.
 */
@Pojo
@Data
@NoArgsConstructor
public class CashSessionPojo {
    private Integer id;
    private Integer userId;
    private String username;
    private String userFullName;
    private CashSessionStatusEnum status;
    private String statusLabel;
    private BigDecimal openingAmount;
    private LocalDateTime openedAt;
    private LocalDateTime closedAt;
    private BigDecimal salesTotal;
    private BigDecimal refundsTotal;
    private BigDecimal incomeTotal;
    private BigDecimal expenseTotal;
    private BigDecimal expectedCash;
    private List<CashMethodTotalPojo> salesByMethod = new ArrayList<>();
    private List<CashMovementPojo> movements = new ArrayList<>();
    private List<CashSummaryPojo> summaries = new ArrayList<>();
    private String closedBy;
    private String closingNote;
    private Long version;
}
