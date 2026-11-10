package bo.cdr.app.appsecurity.presentation.response.pojo;

import bo.cdr.app.common.Pojo;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Datos del usuario autenticado (GET /users/me). */
@Pojo
@Data
@NoArgsConstructor
public class UserInfoPojo {

    private Integer id;
    private String username;
    private String fullName;
    private String email;
    private List<RolePojo> roles;

    public UserInfoPojo(Integer id, String username, String fullName, String email) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.email = email;
    }
}
