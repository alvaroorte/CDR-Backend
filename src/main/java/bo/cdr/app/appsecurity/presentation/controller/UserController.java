package bo.cdr.app.appsecurity.presentation.controller;

import bo.cdr.app.appsecurity.domain.service.interfaces.UserService;
import bo.cdr.app.appsecurity.presentation.request.dto.UserAppDto;
import bo.cdr.app.appsecurity.presentation.request.dto.UserPasswordDto;
import bo.cdr.app.appsecurity.presentation.response.pojo.UserAppPojo;
import bo.cdr.app.appsecurity.presentation.response.pojo.UserInfoPojo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    /** Usuario autenticado (cualquier rol; ver RequestMatcherUtil.PERMISSION_NOT_REQUIRED). */
    @GetMapping("/me")
    public ResponseEntity<UserInfoPojo> getMe() {
        return ResponseEntity.status(HttpStatus.OK).body(userService.getCurrentUserInfo());
    }

    @GetMapping
    public ResponseEntity<List<UserAppPojo>> getAll() {
        return ResponseEntity.status(HttpStatus.OK).body(userService.getAllUserAppPojo());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserAppPojo> getById(@PathVariable Integer id) {
        return ResponseEntity.status(HttpStatus.OK).body(userService.getUserAppPojoById(id));
    }

    @PostMapping
    public ResponseEntity<UserAppPojo> create(@Valid @RequestBody UserAppDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserAppPojo> update(@PathVariable Integer id, @Valid @RequestBody UserAppDto dto) {
        return ResponseEntity.status(HttpStatus.OK).body(userService.update(id, dto));
    }

    @PatchMapping("/{id}/password")
    public ResponseEntity<Void> resetPassword(@PathVariable Integer id, @Valid @RequestBody UserPasswordDto dto) {
        userService.resetPassword(id, dto.getPassword());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    /** Desactiva el usuario (borrado lógico) y revoca sus sesiones. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        userService.softDelete(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
