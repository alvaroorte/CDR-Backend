package bo.cdr.app.appsecurity.presentation.response.pojo;

import bo.cdr.app.common.Pojo;
import lombok.Data;
import lombok.NoArgsConstructor;

@Pojo
@Data
@NoArgsConstructor
public class RolePojo {

    private Integer userId;
    private Integer id;
    private String name;
    private String code;

    public RolePojo(Integer id, String name, String code) {
        this.id = id;
        this.name = name;
        this.code = code;
    }

    public RolePojo(Integer userId, Integer id, String name, String code) {
        this.userId = userId;
        this.id = id;
        this.name = name;
        this.code = code;
    }
}
