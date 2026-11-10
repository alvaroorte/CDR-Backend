package bo.cdr.app.appsecurity.presentation.controller;

import bo.cdr.app.appsecurity.domain.service.UserService;
import bo.cdr.app.appsecurity.presentation.response.pojo.UserInfoPojo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Etapa 1: solo /users/me. El CRUD de usuarios llega en la Etapa 2. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserInfoPojo> getMe() {
        return ResponseEntity.status(HttpStatus.OK).body(userService.getCurrentUserInfo());
    }
}
