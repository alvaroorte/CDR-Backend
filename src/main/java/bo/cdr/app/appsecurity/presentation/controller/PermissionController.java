package bo.cdr.app.appsecurity.presentation.controller;

import bo.cdr.app.appsecurity.domain.service.interfaces.PermissionService;
import bo.cdr.app.appsecurity.presentation.response.pojo.PermissionPojo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Solo lectura: los permisos los genera PermissionSeeder (uno por endpoint) y se asignan desde /roles. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/permission")
public class PermissionController {

    private final PermissionService permissionService;

    @GetMapping
    public ResponseEntity<List<PermissionPojo>> getAll() {
        return ResponseEntity.status(HttpStatus.OK).body(permissionService.getAllActive());
    }
}
