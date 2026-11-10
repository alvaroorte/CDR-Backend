package bo.cdr.app.appsecurity.domain.service;

import bo.cdr.app.appsecurity.data.RoleRepository;
import bo.cdr.app.appsecurity.data.UserRepository;
import bo.cdr.app.appsecurity.data.UserRoleRepository;
import bo.cdr.app.appsecurity.domain.entity.Role;
import bo.cdr.app.appsecurity.domain.entity.UserApp;
import bo.cdr.app.appsecurity.domain.entity.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapTest {

    @Mock UserRepository userRepository;
    @Mock RoleRepository roleRepository;
    @Mock UserRoleRepository userRoleRepository;
    @Mock PasswordEncoder passwordEncoder;

    private AdminBootstrap bootstrap(String password) {
        return new AdminBootstrap(userRepository, roleRepository, userRoleRepository, passwordEncoder, "admin", password);
    }

    @Test
    void run_createsAdminWithHashedPassword_whenNoAdminExists() {
        Role admin = new Role();
        admin.setCode("ADMIN");
        when(userRepository.existsActiveUserWithRole("ADMIN")).thenReturn(false);
        when(userRepository.findByUsernameIgnoreCase("admin")).thenReturn(Optional.empty());
        when(roleRepository.findByCode("ADMIN")).thenReturn(Optional.of(admin));
        when(passwordEncoder.encode("S3creta")).thenReturn("$2a$hash");

        bootstrap("S3creta").run();

        ArgumentCaptor<UserApp> user = ArgumentCaptor.forClass(UserApp.class);
        verify(userRepository).save(user.capture());
        assertThat(user.getValue().getUsername()).isEqualTo("admin");
        assertThat(user.getValue().getPassword()).isEqualTo("$2a$hash");
        ArgumentCaptor<UserRole> userRole = ArgumentCaptor.forClass(UserRole.class);
        verify(userRoleRepository).save(userRole.capture());
        assertThat(userRole.getValue().getRole()).isSameAs(admin);
    }

    @Test
    void run_doesNothing_whenAnAdminAlreadyExists() {
        when(userRepository.existsActiveUserWithRole("ADMIN")).thenReturn(true);

        bootstrap("S3creta").run();

        verify(userRepository, never()).save(any());
    }

    @Test
    void run_doesNothing_whenPasswordVariableIsEmpty() {
        when(userRepository.existsActiveUserWithRole("ADMIN")).thenReturn(false);

        bootstrap("").run();

        verify(userRepository, never()).save(any());
        verifyNoInteractions(roleRepository, passwordEncoder);
    }
}
