package bo.cdr.app.appsecurity.presentation.response.pojo;

import bo.cdr.app.appbase.presentation.response.BasePojo;
import bo.cdr.app.common.Pojo;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Pojo
@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserAppPojo extends BasePojo {
    private Integer id;
    private String username;
    private String fullName;
    private String email;
    private List<RolePojo> roles = new ArrayList<>();

    public UserAppPojo(Integer id, String username, String fullName, String email, Boolean active,
                       String createdBy, String updatedBy, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.email = email;
        this.setActive(active);
        this.setCreatedBy(createdBy);
        this.setUpdatedBy(updatedBy);
        this.setCreatedAt(createdAt);
        this.setUpdatedAt(updatedAt);
    }
}
