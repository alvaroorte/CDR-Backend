package bo.cdr.app.appsecurity.presentation.request.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RefreshTokenDto {

    @NotBlank(message = "{auth.refreshToken.not-blank}")
    private String refreshToken;
}
