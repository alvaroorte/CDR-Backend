package bo.cdr.app.appcatalog.presentation.response.pojo;

import bo.cdr.app.appbase.presentation.response.BasePojo;
import bo.cdr.app.appbase.presentation.response.SelectPojo;
import bo.cdr.app.common.Pojo;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** El listado no incluye la imagen (se carga en GET /product/{id}) para no enviar MBs en la tabla. */
@EqualsAndHashCode(callSuper = true)
@Pojo
@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductPojo extends BasePojo {
    private Integer id;
    private Integer categoryId;
    private String categoryName;
    private String name;
    private String description;
    private String imageBase64;
    private BigDecimal price;
    private Boolean available;
    private Long version;
    private List<SelectPojo> addons = new ArrayList<>();

    public ProductPojo(Integer id, Integer categoryId, String categoryName, String name, String description,
                       String imageBase64, BigDecimal price, Boolean available, Long version, Boolean active,
                       String createdBy, String updatedBy, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.name = name;
        this.description = description;
        this.imageBase64 = imageBase64;
        this.price = price;
        this.available = available;
        this.version = version;
        this.setActive(active);
        this.setCreatedBy(createdBy);
        this.setUpdatedBy(updatedBy);
        this.setCreatedAt(createdAt);
        this.setUpdatedAt(updatedAt);
    }
}
