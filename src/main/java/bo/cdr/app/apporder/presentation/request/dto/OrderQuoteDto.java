package bo.cdr.app.apporder.presentation.request.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/** Carrito para cotizar (POST /order/quote): mismo cálculo que el registro, sin persistir. */
@Data
@NoArgsConstructor
public class OrderQuoteDto {

    @NotEmpty(message = "{order.items.not-empty}")
    @Size(max = 100, message = "{order.items.size}")
    @Valid
    private List<OrderItemDto> items = new ArrayList<>();

    /** Opcional: sin descuento = null. */
    @Valid
    private DiscountDto discount;
}
