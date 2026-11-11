package bo.cdr.app.appcatalog.presentation.response.pojo;

import bo.cdr.app.appbase.presentation.response.BasePojo;
import bo.cdr.app.common.Pojo;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Pojo
@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AddonPojo extends BasePojo {
    private Integer id;
    private String name;
    private BigDecimal price;

    public AddonPojo(Integer id, String name, BigDecimal price, Boolean active,
                     String createdBy, String updatedBy, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.setActive(active);
        this.setCreatedBy(createdBy);
        this.setUpdatedBy(updatedBy);
        this.setCreatedAt(createdAt);
        this.setUpdatedAt(updatedAt);
    }
}
