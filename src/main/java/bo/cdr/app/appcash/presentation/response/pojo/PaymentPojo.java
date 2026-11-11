package bo.cdr.app.appcash.presentation.response.pojo;

import bo.cdr.app.common.Pojo;
import bo.cdr.app.common.enums.PaymentStatusEnum;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Pojo
@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentPojo {
    private Integer id;
    private Integer paymentMethodId;
    private String paymentMethodCode;
    private String paymentMethodName;
    private BigDecimal amount;
    private String reference;
    private PaymentStatusEnum status;
    private String statusLabel;
    private String createdBy;
    private LocalDateTime createdAt;

    public PaymentPojo(Integer id, Integer paymentMethodId, String paymentMethodCode, String paymentMethodName,
                       BigDecimal amount, String reference, PaymentStatusEnum status,
                       String createdBy, LocalDateTime createdAt) {
        this.id = id;
        this.paymentMethodId = paymentMethodId;
        this.paymentMethodCode = paymentMethodCode;
        this.paymentMethodName = paymentMethodName;
        this.amount = amount;
        this.reference = reference;
        this.status = status;
        this.statusLabel = status.getLabel();
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }
}
