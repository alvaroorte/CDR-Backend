package bo.cdr.app.apporder.presentation.response.pojo;

import java.math.BigDecimal;

/** Adicional de una línea (fotografía). Sin @Pojo: es un record (ver CashMethodTotalPojo). */
public record OrderItemAddonPojo(Integer addonId, String addonName, BigDecimal unitPrice) {
}
