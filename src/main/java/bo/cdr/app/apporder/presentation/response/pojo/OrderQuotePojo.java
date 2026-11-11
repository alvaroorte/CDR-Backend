package bo.cdr.app.apporder.presentation.response.pojo;

import bo.cdr.app.common.Pojo;
import bo.cdr.app.common.enums.DiscountTypeEnum;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Resumen calculado por el backend (POST /order/quote). Es lo que el POS muestra en la
 * confirmación: el frontend solo hace vista previa.
 */
@Pojo
@Data
@NoArgsConstructor
public class OrderQuotePojo {
    private List<OrderItemPojo> items = new ArrayList<>();
    private BigDecimal subtotal;
    private DiscountTypeEnum discountType;
    private BigDecimal discountValue;
    private BigDecimal discountAmount;
    /** Porcentaje efectivo del descuento sobre el subtotal (2 decimales). */
    private BigDecimal discountPercent;
    private String discountReason;
    private BigDecimal total;
}
