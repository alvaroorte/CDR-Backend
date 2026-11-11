package bo.cdr.app.appsecurity.domain.service.implement;

import bo.cdr.app.appsecurity.data.RolePermissionRepository;
import bo.cdr.app.appsecurity.data.RoleRepository;
import bo.cdr.app.appsecurity.domain.entity.Permission;
import bo.cdr.app.appsecurity.domain.entity.Role;
import bo.cdr.app.appsecurity.domain.entity.RolePermission;
import bo.cdr.app.appsecurity.domain.mapper.RoleMapper;
import bo.cdr.app.appsecurity.domain.service.interfaces.PermissionService;
import bo.cdr.app.appsecurity.domain.service.interfaces.UserRoleService;
import bo.cdr.app.exception.HasAssociatedEntityException;
import bo.cdr.app.exception.NotAllowedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleServiceImplTest {

    @Mock RoleRepository roleRepository;
    @Mock RolePermissionRepository rolePermissionRepository;
    @Mock PermissionService permissionService;
    @Mock UserRoleService userRoleService;

    RoleServiceImpl service() {
        return new RoleServiceImpl(roleRepository, rolePermissionRepository, new RoleMapper(), permissionService, userRoleService);
    }

    private static Role role(int id, String code) {
        Role r = new Role();
        r.setId(id);
        r.setCode(code);
        r.setName(code);
        r.setActive(true);
        return r;
    }

    @Test
    void softDelete_rejectsSystemRoles() {
        when(roleRepository.findById(2)).thenReturn(Optional.of(role(2, "CAJERO")));
        assertThatThrownBy(() -> service().softDelete(2)).isInstanceOf(NotAllowedException.class);
        verify(roleRepository, never()).save(any());
    }

    @Test
    void softDelete_rejectsRoleAssignedToActiveUsers() {
        when(roleRepository.findById(4)).thenReturn(Optional.of(role(4, "MESERO")));
        when(userRoleService.isAssignedToActiveUsers(4)).thenReturn(true);
        assertThatThrownBy(() -> service().softDelete(4)).isInstanceOf(HasAssociatedEntityException.class);
    }

    @Test
    void softDelete_deactivatesCustomRoleWithoutUsers() {
        Role mesero = role(4, "MESERO");
        when(roleRepository.findById(4)).thenReturn(Optional.of(mesero));
        when(userRoleService.isAssignedToActiveUsers(4)).thenReturn(false);

        service().softDelete(4);

        assertThat(mesero.getActive()).isFalse();
    }

    @Test
    void replacePermissions_deletesPreviousAndSavesNewSet() {
        Role cajero = role(2, "CAJERO");
        Permission p1 = new Permission();
        p1.setId(10);
        Permission p2 = new Permission();
        p2.setId(11);
        when(roleRepository.findById(2)).thenReturn(Optional.of(cajero));
        when(permissionService.getActiveByIds(List.of(10, 11))).thenReturn(List.of(p1, p2));
        when(rolePermissionRepository.findPermissionIdsByRoleId(2)).thenReturn(List.of(10, 11));

        List<Integer> result = service().replacePermissions(2, List.of(10, 11));

        verify(rolePermissionRepository).deleteByRoleId(2);
        verify(rolePermissionRepository, times(2)).save(any(RolePermission.class));
        assertThat(result).containsExactly(10, 11);
    }
}
