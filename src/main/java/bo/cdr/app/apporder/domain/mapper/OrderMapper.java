package bo.cdr.app.apporder.domain.mapper;

import bo.cdr.app.appcash.domain.entity.CashSession;
import bo.cdr.app.appcash.presentation.response.pojo.PaymentPojo;
import bo.cdr.app.appcatalog.domain.entity.TableMarker;
import bo.cdr.app.apporder.domain.entity.Order;
import bo.cdr.app.apporder.domain.entity.OrderItem;
import bo.cdr.app.apporder.domain.entity.OrderItemAddon;
import bo.cdr.app.apporder.domain.entity.OrderStatusHistory;
import bo.cdr.app.apporder.presentation.response.pojo.OrderBoardPojo;
import bo.cdr.app.apporder.presentation.response.pojo.OrderListPojo;
import bo.cdr.app.apporder.presentation.response.pojo.OrderStatusHistoryPojo;
import bo.cdr.app.apporder.domain.model.PricedOrder;
import bo.cdr.app.apporder.presentation.response.pojo.OrderItemAddonPojo;
import bo.cdr.app.apporder.presentation.response.pojo.OrderItemPojo;
import bo.cdr.app.apporder.presentation.response.pojo.OrderPojo;
import bo.cdr.app.apporder.presentation.response.pojo.OrderQuotePojo;
import bo.cdr.app.common.enums.OrderStatusEnum;
import bo.cdr.app.common.enums.ServiceTypeEnum;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/** Copia las fotografías del cálculo al pedido y arma las respuestas. */
@Component
public class OrderMapper {

    public static String codeLabel(Integer code) {
        return String.format("#%03d", code);
    }

    /** Pedido nuevo en PENDING con detalle fotografiado. El código y la fecha los pone el servicio. */
    public Order fromPriced(PricedOrder priced, ServiceTypeEnum serviceType, TableMarker marker, String customerName,
                            CashSession cashSession, String username, LocalDateTime now) {
        Order order = new Order();
        order.setServiceType(serviceType);
        order.setTableMarker(marker);
        order.setTableMarkerNumber(marker == null ? null : marker.getNumber());
        order.setCustomerName(customerName);
        order.setStatus(OrderStatusEnum.PENDING);
        order.setSubtotal(priced.subtotal());
        order.setDiscountAmount(priced.discountAmount());
        order.setTotal(priced.total());
        order.setCashSession(cashSession);
        if (priced.hasDiscount()) {
            order.setDiscountType(priced.discountType());
            order.setDiscountValue(priced.discountValue());
            order.setDiscountReason(priced.discountReason());
            order.setDiscountAppliedBy(username);
            order.setDiscountAppliedAt(now);
        }
        for (PricedOrder.Line line : priced.lines()) {
            OrderItem item = new OrderItem();
            item.setProductId(line.product().getId());
            item.setProductName(line.product().getName());
            item.setCategoryId(line.categoryId());
            item.setCategoryName(line.categoryName());
            item.setQuantity(line.quantity());
            item.setUnitPrice(line.unitPrice());
            item.setAddonsUnitTotal(line.addonsUnitTotal());
            item.setLineSubtotal(line.lineSubtotal());
            item.setNotes(line.notes());
            line.addons().forEach(addon -> {
                OrderItemAddon snapshot = new OrderItemAddon();
                snapshot.setAddonId(addon.getId());
                snapshot.setAddonName(addon.getName());
                snapshot.setUnitPrice(addon.getPrice());
                item.addAddon(snapshot);
            });
            order.addItem(item);
        }
        return order;
    }

    public OrderQuotePojo toQuotePojo(PricedOrder priced) {
        OrderQuotePojo pojo = new OrderQuotePojo();
        pojo.setItems(priced.lines().stream().map(line -> new OrderItemPojo(
                null, line.product().getId(), line.product().getName(), line.categoryName(), line.quantity(),
                line.unitPrice(), line.addonsUnitTotal(), line.lineSubtotal(), line.notes(),
                line.addons().stream().map(a -> new OrderItemAddonPojo(a.getId(), a.getName(), a.getPrice())).toList()
        )).toList());
        pojo.setSubtotal(priced.subtotal());
        pojo.setDiscountType(priced.discountType());
        pojo.setDiscountValue(priced.discountValue());
        pojo.setDiscountAmount(priced.discountAmount());
        pojo.setDiscountPercent(priced.discountPercent());
        pojo.setDiscountReason(priced.discountReason());
        pojo.setTotal(priced.total());
        return pojo;
    }

    public OrderPojo toPojo(Order order, List<PaymentPojo> payments) {
        OrderPojo pojo = new OrderPojo();
        pojo.setId(order.getId());
        pojo.setCode(order.getCode());
        pojo.setCodeLabel(codeLabel(order.getCode()));
        pojo.setBusinessDate(order.getBusinessDate());
        pojo.setServiceType(order.getServiceType());
        pojo.setServiceTypeLabel(order.getServiceType().getLabel());
        pojo.setTableMarkerId(order.getTableMarker() == null ? null : order.getTableMarker().getId());
        pojo.setTableMarkerNumber(order.getTableMarkerNumber());
        pojo.setCustomerName(order.getCustomerName());
        pojo.setStatus(order.getStatus());
        pojo.setStatusLabel(order.getStatus().getLabel());
        pojo.setSubtotal(order.getSubtotal());
        pojo.setDiscountType(order.getDiscountType());
        pojo.setDiscountValue(order.getDiscountValue());
        pojo.setDiscountAmount(order.getDiscountAmount());
        pojo.setDiscountReason(order.getDiscountReason());
        pojo.setDiscountAppliedBy(order.getDiscountAppliedBy());
        pojo.setTotal(order.getTotal());
        pojo.setCashSessionId(order.getCashSession().getId());
        pojo.setItems(order.getItems().stream().map(item -> new OrderItemPojo(
                item.getId(), item.getProductId(), item.getProductName(), item.getCategoryName(), item.getQuantity(),
                item.getUnitPrice(), item.getAddonsUnitTotal(), item.getLineSubtotal(), item.getNotes(),
                item.getAddons().stream()
                        .map(a -> new OrderItemAddonPojo(a.getAddonId(), a.getAddonName(), a.getUnitPrice())).toList()
        )).toList());
        pojo.setPayments(payments);
        pojo.setCancelReason(order.getCancelReason());
        pojo.setCancelledBy(order.getCancelledBy());
        pojo.setCancelledAt(order.getCancelledAt());
        pojo.setCreatedBy(order.getCreatedBy());
        pojo.setCreatedAt(order.getCreatedAt());
        pojo.setVersion(order.getVersion());
        return pojo;
    }

    /** Tablero de cocina / seguimiento: lee las fotografías del detalle. */
    public OrderBoardPojo toBoardPojo(Order order) {
        return new OrderBoardPojo(order.getId(), order.getCode(), codeLabel(order.getCode()), order.getServiceType(),
                order.getServiceType().getLabel(), order.getTableMarkerNumber(), order.getCustomerName(),
                order.getStatus(), order.getStatus().getLabel(), order.getTotal(), order.getVersion(),
                order.getCreatedAt(), order.getCreatedBy(), order.getUpdatedAt(),
                order.getItems().stream().map(item -> new OrderBoardPojo.Item(item.getProductName(), item.getQuantity(),
                        item.getNotes(), item.getAddons().stream().map(OrderItemAddon::getAddonName).toList())).toList());
    }

    public OrderListPojo toListPojo(Order order) {
        return new OrderListPojo(order.getId(), codeLabel(order.getCode()), order.getBusinessDate(), order.getCreatedAt(),
                order.getServiceType(), order.getServiceType().getLabel(), order.getTableMarkerNumber(),
                order.getCustomerName(), order.getStatus(), order.getStatus().getLabel(), order.getSubtotal(),
                order.getDiscountAmount(), order.getTotal(), order.getCreatedBy(), order.getVersion());
    }

    public OrderStatusHistoryPojo toHistoryPojo(OrderStatusHistory history) {
        return new OrderStatusHistoryPojo(history.getFromStatus(),
                history.getFromStatus() == null ? null : history.getFromStatus().getLabel(),
                history.getToStatus(), history.getToStatus().getLabel(), history.getChangedBy(),
                history.getChangedAt(), history.getNote());
    }
}
