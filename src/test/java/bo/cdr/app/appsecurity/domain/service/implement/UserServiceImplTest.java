package bo.cdr.app.appsecurity.domain.service.implement;

import bo.cdr.app.appsecurity.auth.jwt.JwtService;
import bo.cdr.app.appsecurity.data.UserRepository;
import bo.cdr.app.appsecurity.domain.entity.Role;
import bo.cdr.app.appsecurity.domain.entity.UserApp;
import bo.cdr.app.appsecurity.domain.entity.UserRole;
import bo.cdr.app.appsecurity.domain.mapper.UserAppMapper;
import bo.cdr.app.appsecurity.domain.service.interfaces.RefreshTokenService;
import bo.cdr.app.appsecurity.domain.service.interfaces.RoleService;
import bo.cdr.app.appsecurity.domain.service.interfaces.UserRoleService;
import bo.cdr.app.appsecurity.presentation.request.dto.UserAppDto;
import bo.cdr.app.appsecurity.presentation.response.pojo.UserAppPojo;
import bo.cdr.app.exception.DuplicateException;
import bo.cdr.app.exception.NotAllowedException;
import bo.cdr.app.exception.response.ValidationErrorException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock UserRepository userRepository;
    @Mock UserRoleService userRoleService;
    @Mock RoleService roleService;
    @Mock RefreshTokenService refreshTokenService;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;

    UserServiceImpl service() {
        return new UserServiceImpl(userRepository, userRoleService, roleService, refreshTokenService,
                new UserAppMapper(), passwordEncoder, jwtService);
    }

    private static Role role(int id, String code) {
        Role r = new Role();
        r.setId(id);
        r.setCode(code);
        return r;
    }

    private static UserApp user(int id, String username) {
        UserApp u = new UserApp();
        u.setId(id);
        u.setUsername(username);
        u.setFullName(username);
        u.setActive(true);
        return u;
    }

    private static UserAppDto dto(String username, String password, boolean active, Integer... roleIds) {
        UserAppDto dto = new UserAppDto();
        dto.setUsername(username);
        dto.setFullName("Nombre " + username);
        dto.setPassword(password);
        dto.setActive(active);
        dto.setRoleIds(List.of(roleIds));
        return dto;
    }

    private void givenAdmin(UserApp user) {
        UserRole ur = new UserRole();
        ur.setRole(role(1, "ADMIN"));
        when(userRoleService.getRolesByUser(user)).thenReturn(List.of(ur));
    }

    @Test
    void create_hashesPassword_andAssignsRoles() {
        Role cajero = role(2, "CAJERO");
        when(roleService.getActiveByIds(List.of(2))).thenReturn(List.of(cajero));
        when(passwordEncoder.encode("Caja2026!")).thenReturn("$2a$hash");
        when(userRepository.save(any(UserApp.class))).thenAnswer(inv -> { UserApp u = inv.getArgument(0); u.setId(7); return u; });
        when(userRepository.getUserAppPojoById(7)).thenReturn(Optional.of(new UserAppPojo()));

        service().create(dto(" maria.caja ", "Caja2026!", true, 2));

        ArgumentCaptor<UserApp> saved = ArgumentCaptor.forClass(UserApp.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getUsername()).isEqualTo("maria.caja");
        assertThat(saved.getValue().getPassword()).isEqualTo("$2a$hash");
        verify(userRoleService).replaceRoles(saved.getValue(), List.of(cajero));
    }

    @Test
    void create_requiresPassword() {
        assertThatThrownBy(() -> service().create(dto("maria", null, true, 2)))
                .isInstanceOf(ValidationErrorException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void create_rejectsDuplicateUsername() {
        when(userRepository.existsByUsernameIgnoreCase("maria")).thenReturn(true);
        assertThatThrownBy(() -> service().create(dto("maria", "Clave1234", true, 2)))
                .isInstanceOf(DuplicateException.class);
    }

    @Test
    void softDelete_rejectsDeactivatingSelf() {
        UserApp me = user(1, "admin");
        when(userRepository.findById(1)).thenReturn(Optional.of(me));
        when(jwtService.getCurrentUserId()).thenReturn(1);

        assertThatThrownBy(() -> service().softDelete(1)).isInstanceOf(NotAllowedException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void softDelete_rejectsDeactivatingLastAdmin() {
        UserApp other = user(5, "otro.admin");
        when(userRepository.findById(5)).thenReturn(Optional.of(other));
        when(jwtService.getCurrentUserId()).thenReturn(1);
        givenAdmin(other);
        when(userRepository.countActiveUsersWithRoleExcluding("ADMIN", 5)).thenReturn(0L);

        assertThatThrownBy(() -> service().softDelete(5)).isInstanceOf(NotAllowedException.class);
    }

    @Test
    void softDelete_deactivatesAndRevokesSessions() {
        UserApp cajero = user(7, "maria.caja");
        when(userRepository.findById(7)).thenReturn(Optional.of(cajero));
        when(jwtService.getCurrentUserId()).thenReturn(1);
        when(userRoleService.getRolesByUser(cajero)).thenReturn(List.of());

        service().softDelete(7);

        assertThat(cajero.getActive()).isFalse();
        verify(refreshTokenService).revokeAllForUser("maria.caja");
    }

    @Test
    void update_rejectsRemovingAdminRoleFromLastAdmin() {
        UserApp admin = user(1, "admin");
        when(userRepository.findById(1)).thenReturn(Optional.of(admin));
        when(roleService.getActiveByIds(List.of(2))).thenReturn(List.of(role(2, "CAJERO")));
        givenAdmin(admin);
        when(userRepository.countActiveUsersWithRoleExcluding("ADMIN", 1)).thenReturn(0L);

        assertThatThrownBy(() -> service().update(1, dto("admin", null, true, 2)))
                .isInstanceOf(NotAllowedException.class);
        verify(userRoleService, never()).replaceRoles(any(), any());
    }

    @Test
    void update_ignoresPassword() {
        UserApp cajero = user(7, "maria.caja");
        cajero.setPassword("$2a$original");
        when(userRepository.findById(7)).thenReturn(Optional.of(cajero));
        when(roleService.getActiveByIds(List.of(2))).thenReturn(List.of(role(2, "CAJERO")));
        when(userRepository.getUserAppPojoById(7)).thenReturn(Optional.of(new UserAppPojo()));

        service().update(7, dto("maria.caja", "OtraClave99", true, 2));

        assertThat(cajero.getPassword()).isEqualTo("$2a$original");
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void resetPassword_hashesAndRevokesSessions() {
        UserApp cajero = user(7, "maria.caja");
        when(userRepository.findById(7)).thenReturn(Optional.of(cajero));
        when(passwordEncoder.encode("NuevaClave1")).thenReturn("$2a$nuevo");

        service().resetPassword(7, "NuevaClave1");

        assertThat(cajero.getPassword()).isEqualTo("$2a$nuevo");
        verify(refreshTokenService).revokeAllForUser("maria.caja");
    }
}
