package bo.cdr.app.apporder.presentation.request.dto;

import bo.cdr.app.config.IntegerListDeserializer;
import bo.cdr.app.config.IntegerOnlyDeserializer;
import bo.cdr.app.config.StringOnlyDeserializer;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.util.ArrayList;
import java.util.List;

/** Línea del carrito: solo ids y cantidades. Los precios los pone el backend. */
@Data
@NoArgsConstructor
public class OrderItemDto {

    @NotNull(message = "{order.item.productId.not-null}")
    @Positive(message = "{order.item.productId.not-null}")
    @JsonDeserialize(using = IntegerOnlyDeserializer.class)
    private Integer productId;

    @NotNull(message = "{order.item.quantity.not-null}")
    @Min(value = 1, message = "{order.item.quantity.min}")
    @Max(value = 999, message = "{order.item.quantity.max}")
    @JsonDeserialize(using = IntegerOnlyDeserializer.class)
    private Integer quantity;

    @JsonDeserialize(using = IntegerListDeserializer.class)
    private List<Integer> addonIds = new ArrayList<>();

    @JsonDeserialize(using = StringOnlyDeserializer.class)
    @Size(max = 255, message = "{order.item.notes.size}")
    private String notes;
}
