package bo.cdr.app.appsecurity.domain.service;

import bo.cdr.app.appsecurity.data.RoleRepository;
import bo.cdr.app.appsecurity.data.UserRepository;
import bo.cdr.app.appsecurity.data.UserRoleRepository;
import bo.cdr.app.appsecurity.domain.entity.Role;
import bo.cdr.app.appsecurity.domain.entity.UserApp;
import bo.cdr.app.appsecurity.domain.entity.UserRole;
import bo.cdr.app.util.constants.RoleCodes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Crea el usuario ADMIN inicial en el primer arranque (decisión aprobada): solo si no existe
 * ningún usuario activo con rol ADMIN y la variable APP_ADMIN_INITIAL_PASSWORD tiene valor.
 * La contraseña nunca se guarda en una migración ni en el repositorio (salvo el valor local de dev).
 */
@Slf4j
@Component
public class AdminBootstrap implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final String initialUsername;
    private final String initialPassword;

    public AdminBootstrap(UserRepository userRepository,
                          RoleRepository roleRepository,
                          UserRoleRepository userRoleRepository,
                          PasswordEncoder passwordEncoder,
                          @Value("${app.admin.initial-username:admin}") String initialUsername,
                          @Value("${app.admin.initial-password:}") String initialPassword) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
        this.initialUsername = initialUsername;
        this.initialPassword = initialPassword;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void run(String... args) {
        if (userRepository.existsActiveUserWithRole(RoleCodes.ADMIN)) {
            return;
        }
        if (initialPassword == null || initialPassword.isBlank()) {
            log.warn("No existe ningún ADMIN y APP_ADMIN_INITIAL_PASSWORD está vacía: no se crea el usuario inicial.");
            return;
        }
        if (userRepository.findByUsernameIgnoreCase(initialUsername).isPresent()) {
            log.warn("El usuario '{}' ya existe pero no tiene rol ADMIN activo; no se modifica.", initialUsername);
            return;
        }

        Role adminRole = roleRepository.findByCode(RoleCodes.ADMIN)
                .orElseThrow(() -> new IllegalStateException("No existe el rol ADMIN (migración V2)"));

        UserApp admin = new UserApp();
        admin.setUsername(initialUsername);
        admin.setFullName("Administrador");
        admin.setPassword(passwordEncoder.encode(initialPassword));
        userRepository.save(admin);

        UserRole userRole = new UserRole();
        userRole.setUser(admin);
        userRole.setRole(adminRole);
        userRoleRepository.save(userRole);

        log.info("Usuario ADMIN inicial '{}' creado.", initialUsername);
    }
}
