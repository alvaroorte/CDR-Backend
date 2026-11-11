package bo.cdr.app.appcash.presentation.response.pojo;

import bo.cdr.app.common.Pojo;
import bo.cdr.app.common.enums.CashMovementTypeEnum;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Movimiento de caja. {@code amount} siempre positivo; el tipo indica si suma o resta. */
@Pojo
@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CashMovementPojo {
    private Integer id;
    private CashMovementTypeEnum type;
    private String typeLabel;
    private Integer paymentMethodId;
    private String paymentMethodCode;
    private String paymentMethodName;
    private BigDecimal amount;
    private Integer orderId;
    private String description;
    private String createdBy;
    private LocalDateTime createdAt;

    public CashMovementPojo(Integer id, CashMovementTypeEnum type, Integer paymentMethodId, String paymentMethodCode,
                            String paymentMethodName, BigDecimal amount, Integer orderId, String description,
                            String createdBy, LocalDateTime createdAt) {
        this.id = id;
        this.type = type;
        this.typeLabel = type.getLabel();
        this.paymentMethodId = paymentMethodId;
        this.paymentMethodCode = paymentMethodCode;
        this.paymentMethodName = paymentMethodName;
        this.amount = amount;
        this.orderId = orderId;
        this.description = description;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }
}
