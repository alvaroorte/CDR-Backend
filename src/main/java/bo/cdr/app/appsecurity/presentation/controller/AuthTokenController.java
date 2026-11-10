package bo.cdr.app.appsecurity.presentation.controller;

import bo.cdr.app.appsecurity.auth.service.AuthService;
import bo.cdr.app.appsecurity.presentation.request.dto.RefreshTokenDto;
import bo.cdr.app.appsecurity.presentation.response.pojo.AuthPojo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthTokenController {

    private final AuthService authService;

    @PostMapping("/refresh")
    public ResponseEntity<AuthPojo> refresh(@Valid @RequestBody RefreshTokenDto request) {
        return ResponseEntity.ok(authService.refresh(request.getRefreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenDto request) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.ok().build();
    }
}
