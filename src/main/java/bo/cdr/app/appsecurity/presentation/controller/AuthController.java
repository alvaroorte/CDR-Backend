package bo.cdr.app.appsecurity.presentation.controller;

import bo.cdr.app.appsecurity.auth.service.AuthService;
import bo.cdr.app.appsecurity.presentation.request.dto.UserCredentialsDto;
import bo.cdr.app.appsecurity.presentation.response.pojo.AuthPojo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/login")
public class AuthController {

    private final AuthService authService;

    @PostMapping
    public ResponseEntity<AuthPojo> login(@Valid @RequestBody UserCredentialsDto request,
                                          HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authService.login(request, httpRequest.getRemoteAddr()));
    }
}
