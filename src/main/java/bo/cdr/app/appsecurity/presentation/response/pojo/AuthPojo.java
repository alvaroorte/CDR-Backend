package bo.cdr.app.appsecurity.presentation.response.pojo;

import bo.cdr.app.common.Pojo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Pojo
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthPojo {
    private String token;
    private String message;
    private String username;
    private String refreshToken;
}
