package bo.cdr.app.apporder.presentation.request.dto;

import bo.cdr.app.appcash.presentation.request.dto.PaymentDto;
import bo.cdr.app.common.custom.validator.ValidEnum;
import bo.cdr.app.common.enums.ServiceTypeEnum;
import bo.cdr.app.config.IntegerOnlyDeserializer;
import bo.cdr.app.config.StringOnlyDeserializer;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.util.ArrayList;
import java.util.List;

/** Registro del pedido (POST /order): carrito + modalidad + pagos, en una sola transacción. */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class OrderDto extends OrderQuoteDto {

    @ValidEnum(enumClass = ServiceTypeEnum.class, message = "{order.serviceType.invalid}")
    @JsonDeserialize(using = StringOnlyDeserializer.class)
    private String serviceType;

    /** Obligatorio en DINE_IN, prohibido en TAKEAWAY (lo valida el servicio). */
    @Positive(message = "{order.tableMarkerId.positive}")
    @JsonDeserialize(using = IntegerOnlyDeserializer.class)
    private Integer tableMarkerId;

    @JsonDeserialize(using = StringOnlyDeserializer.class)
    @Size(max = 100, message = "{order.customerName.size}")
    private String customerName;

    @NotEmpty(message = "{order.payments.not-empty}")
    @Size(max = 10, message = "{order.payments.size}")
    @Valid
    private List<PaymentDto> payments = new ArrayList<>();
}
