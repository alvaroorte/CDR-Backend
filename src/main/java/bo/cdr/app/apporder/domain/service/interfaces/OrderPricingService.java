package bo.cdr.app.apporder.domain.service.interfaces;

import bo.cdr.app.apporder.domain.model.PricedOrder;
import bo.cdr.app.apporder.presentation.request.dto.OrderQuoteDto;

/**
 * ÚNICO lugar con el cálculo de importes del pedido (skill cdr-order-management §4–§5).
 * Ignora cualquier precio del cliente: lee el catálogo vigente.
 */
public interface OrderPricingService {

    /**
     * Calcula líneas, subtotal, descuento y total, validando disponibilidad, adicionales,
     * límites de descuento por rol del usuario autenticado, motivo y total &gt; 0.
     */
    PricedOrder price(OrderQuoteDto dto);
}
