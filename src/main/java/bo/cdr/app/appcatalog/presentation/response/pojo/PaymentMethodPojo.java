package bo.cdr.app.appcatalog.presentation.response.pojo;

import bo.cdr.app.appbase.presentation.response.BasePojo;
import bo.cdr.app.common.Pojo;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Pojo
@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentMethodPojo extends BasePojo {
    private Integer id;
    private String code;
    private String name;
    private Boolean requiresReference;
    private Boolean givesChange;

    public PaymentMethodPojo(Integer id, String code, String name, Boolean requiresReference, Boolean givesChange,
                             Boolean active, String createdBy, String updatedBy,
                             LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.requiresReference = requiresReference;
        this.givesChange = givesChange;
        this.setActive(active);
        this.setCreatedBy(createdBy);
        this.setUpdatedBy(updatedBy);
        this.setCreatedAt(createdAt);
        this.setUpdatedAt(updatedAt);
    }
}
