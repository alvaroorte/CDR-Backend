package bo.cdr.app.appcash.presentation.response.pojo;

import bo.cdr.app.common.enums.OrderStatusEnum;

/** Pedido cobrado en la caja que aún no se entregó (advertencia al cerrar). */
public record CashOpenOrderPojo(Integer id, String codeLabel, OrderStatusEnum status, String statusLabel) {

    /** Constructor para la proyección JPQL. */
    public CashOpenOrderPojo(Integer id, Integer code, OrderStatusEnum status) {
        this(id, String.format("#%03d", code), status, status.getLabel());
    }
}
