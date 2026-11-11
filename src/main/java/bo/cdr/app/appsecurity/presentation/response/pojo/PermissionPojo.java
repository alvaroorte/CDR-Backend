package bo.cdr.app.appsecurity.presentation.response.pojo;

import bo.cdr.app.common.Pojo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Pojo
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PermissionPojo {
    private Integer id;
    private String code;
    private String name;
    private String description;
    private String httpMethod;
    private String url;
}
