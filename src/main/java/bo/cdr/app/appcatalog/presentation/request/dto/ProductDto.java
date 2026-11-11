package bo.cdr.app.appcatalog.presentation.request.dto;

import bo.cdr.app.common.custom.validator.IsImageBase64;
import bo.cdr.app.config.IntegerListDeserializer;
import bo.cdr.app.config.IntegerOnlyDeserializer;
import bo.cdr.app.config.StringOnlyDeserializer;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class ProductDto {

    @NotNull(message = "{product.categoryId.not-null}")
    @JsonDeserialize(using = IntegerOnlyDeserializer.class)
    @Positive(message = "{product.categoryId.positive}")
    private Integer categoryId;

    @NotBlank(message = "{product.name.not-blank}")
    @JsonDeserialize(using = StringOnlyDeserializer.class)
    @Size(min = 2, max = 100, message = "{product.name.size}")
    private String name;

    @JsonDeserialize(using = StringOnlyDeserializer.class)
    @Size(max = 500, message = "{common.description.size}")
    private String description;

    @IsImageBase64(message = "{common.image.invalid}")
    private String imageBase64;

    @NotNull(message = "{common.price.not-null}")
    @DecimalMin(value = "0.00", message = "{common.price.min}")
    @Digits(integer = 10, fraction = 2, message = "{common.price.digits}")
    private BigDecimal price;

    @NotNull(message = "{product.available.not-null}")
    private Boolean available;

    @NotNull(message = "{common.active.not-null}")
    private Boolean active;

    @JsonDeserialize(using = IntegerListDeserializer.class)
    private List<Integer> addonIds = new ArrayList<>();
}
