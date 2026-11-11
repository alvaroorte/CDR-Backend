package bo.cdr.app.appsecurity.presentation.response.pojo;

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
public class RolePojo extends BasePojo {

    private Integer userId;
    private Integer id;
    private String name;
    private String code;
    private String description;
    /** Rol del sistema (ADMIN, CAJERO, COCINA): no se borra ni cambia de código. */
    private Boolean system;

    public RolePojo(Integer id, String name, String code) {
        this.id = id;
        this.name = name;
        this.code = code;
    }

    public RolePojo(Integer userId, Integer id, String name, String code) {
        this(id, name, code);
        this.userId = userId;
    }

    public RolePojo(Integer id, String name, String code, String description, Boolean active,
                    String createdBy, String updatedBy, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this(id, name, code);
        this.description = description;
        this.setActive(active);
        this.setCreatedBy(createdBy);
        this.setUpdatedBy(updatedBy);
        this.setCreatedAt(createdAt);
        this.setUpdatedAt(updatedAt);
    }
}
