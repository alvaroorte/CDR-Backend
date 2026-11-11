package bo.cdr.app.apporder.presentation.response.pojo;

import bo.cdr.app.common.enums.OrderStatusEnum;
import bo.cdr.app.common.enums.ServiceTypeEnum;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Pedido tal como lo muestran el tablero de cocina y el seguimiento (sin pagos ni importes de
 * línea). {@code statusChangedAt} = última modificación del pedido. Record sin @Pojo.
 */
public record OrderBoardPojo(Integer id,
                             Integer code,
                             String codeLabel,
                             ServiceTypeEnum serviceType,
                             String serviceTypeLabel,
                             Integer tableMarkerNumber,
                             String customerName,
                             OrderStatusEnum status,
                             String statusLabel,
                             BigDecimal total,
                             Long version,
                             LocalDateTime createdAt,
                             String createdBy,
                             LocalDateTime statusChangedAt,
                             List<Item> items) {

    public record Item(String productName, Integer quantity, String notes, List<String> addons) {
    }
}
