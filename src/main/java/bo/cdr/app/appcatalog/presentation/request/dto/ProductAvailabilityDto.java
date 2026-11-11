package bo.cdr.app.appcatalog.presentation.request.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Marcar un producto como agotado/disponible (ADMIN y CAJERO). */
@Data
@NoArgsConstructor
public class ProductAvailabilityDto {

    @NotNull(message = "{product.available.not-null}")
    private Boolean available;
}
