package bo.cdr.app.appsecurity.presentation.controller;

import bo.cdr.app.appsecurity.domain.service.interfaces.RoleService;
import bo.cdr.app.appsecurity.presentation.request.dto.RoleDto;
import bo.cdr.app.appsecurity.presentation.request.dto.RolePermissionsDto;
import bo.cdr.app.appsecurity.presentation.response.pojo.RolePojo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/roles")
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    public ResponseEntity<List<RolePojo>> getAll() {
        return ResponseEntity.status(HttpStatus.OK).body(roleService.getAllRolePojo());
    }

    @GetMapping("/select")
    public ResponseEntity<List<RolePojo>> getSelect() {
        return ResponseEntity.status(HttpStatus.OK).body(roleService.getSelect());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RolePojo> getById(@PathVariable Integer id) {
        return ResponseEntity.status(HttpStatus.OK).body(roleService.getRolePojoById(id));
    }

    @PostMapping
    public ResponseEntity<RolePojo> create(@Valid @RequestBody RoleDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roleService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RolePojo> update(@PathVariable Integer id, @Valid @RequestBody RoleDto dto) {
        return ResponseEntity.status(HttpStatus.OK).body(roleService.update(id, dto));
    }

    /** Desactiva el rol. No permitido para roles del sistema ni con usuarios activos. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        roleService.softDelete(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping("/{id}/permissions")
    public ResponseEntity<List<Integer>> getPermissions(@PathVariable Integer id) {
        return ResponseEntity.status(HttpStatus.OK).body(roleService.getPermissionIds(id));
    }

    @PutMapping("/{id}/permissions")
    public ResponseEntity<List<Integer>> replacePermissions(@PathVariable Integer id,
                                                            @Valid @RequestBody RolePermissionsDto dto) {
        return ResponseEntity.status(HttpStatus.OK).body(roleService.replacePermissions(id, dto.getPermissionIds()));
    }
}
