package bo.cdr.app.apporder.presentation.response.pojo;

import bo.cdr.app.common.Pojo;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Línea del pedido (cotización o pedido registrado), con importes calculados por el backend. */
@Pojo
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderItemPojo {
    private Integer id;
    private Integer productId;
    private String productName;
    private String categoryName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal addonsUnitTotal;
    private BigDecimal lineSubtotal;
    private String notes;
    private List<OrderItemAddonPojo> addons = new ArrayList<>();
}
