package bo.cdr.app.appsecurity.domain.service.implement;

import bo.cdr.app.appbase.data.repository.GenericRepository;
import bo.cdr.app.appbase.domain.service.CRUDServiceImpl;
import bo.cdr.app.appsecurity.auth.jwt.JwtService;
import bo.cdr.app.appsecurity.data.UserRepository;
import bo.cdr.app.appsecurity.domain.entity.Role;
import bo.cdr.app.appsecurity.domain.entity.UserApp;
import bo.cdr.app.appsecurity.domain.mapper.UserAppMapper;
import bo.cdr.app.appsecurity.domain.service.interfaces.RefreshTokenService;
import bo.cdr.app.appsecurity.domain.service.interfaces.RoleService;
import bo.cdr.app.appsecurity.domain.service.interfaces.UserRoleService;
import bo.cdr.app.appsecurity.domain.service.interfaces.UserService;
import bo.cdr.app.appsecurity.presentation.request.dto.UserAppDto;
import bo.cdr.app.appsecurity.presentation.response.pojo.RolePojo;
import bo.cdr.app.appsecurity.presentation.response.pojo.UserAppPojo;
import bo.cdr.app.appsecurity.presentation.response.pojo.UserInfoPojo;
import bo.cdr.app.exception.DuplicateException;
import bo.cdr.app.exception.EntityNotFoundException;
import bo.cdr.app.exception.NotAllowedException;
import bo.cdr.app.exception.response.ValidationErrorException;
import bo.cdr.app.util.constants.RoleCodes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Reglas de usuarios:
 * - username y email únicos (sin distinguir mayúsculas);
 * - nadie puede desactivarse a sí mismo;
 * - el sistema nunca se queda sin un ADMIN activo;
 * - al desactivar un usuario o restablecer su contraseña se revocan sus sesiones (refresh tokens).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends CRUDServiceImpl<UserApp, Integer> implements UserService {

    private final UserRepository userRepository;
    private final UserRoleService userRoleService;
    private final RoleService roleService;
    private final RefreshTokenService refreshTokenService;
    private final UserAppMapper userAppMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    protected GenericRepository<UserApp, Integer> getRepository() {
        return userRepository;
    }

    @Override
    public Optional<UserApp> findActiveByUsername(String username) {
        return userRepository.findByUsernameIgnoreCaseAndActiveIsTrue(username);
    }

    @Override
    @Transactional(readOnly = true)
    public UserInfoPojo getCurrentUserInfo() {
        Integer userId = jwtService.getCurrentUserId();
        UserInfoPojo info = userRepository.getUserInfoById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Usuario", userId));
        info.setRoles(userRoleService.getRolesByUserId(userId));
        return info;
    }

    @Override
    public boolean hasPermission(Integer userId, String method, String url) {
        return userRepository.hasPermission(userId, method, url);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserAppPojo> getAllUserAppPojo() {
        List<UserAppPojo> users = userRepository.findAllUserAppPojo();
        attachRoles(users);
        return users;
    }

    @Override
    @Transactional(readOnly = true)
    public UserAppPojo getUserAppPojoById(Integer id) {
        UserAppPojo user = userRepository.getUserAppPojoById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuario", id));
        attachRoles(List.of(user));
        return user;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserAppPojo create(UserAppDto dto) {
        if (dto.getPassword() == null || dto.getPassword().isBlank()) {
            throw new ValidationErrorException("La contraseña es obligatoria al crear un usuario.");
        }
        validateUnique(dto, null);
        List<Role> roles = roleService.getActiveByIds(dto.getRoleIds());

        UserApp user = userAppMapper.fromDto(dto, null);
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        userRepository.save(user);
        userRoleService.replaceRoles(user, roles);

        log.info("Usuario creado: {} con roles {}", user.getUsername(), roleCodes(roles));
        return getUserAppPojoById(user.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserAppPojo update(Integer id, UserAppDto dto) {
        UserApp user = getById(id);
        validateUnique(dto, id);
        List<Role> roles = roleService.getActiveByIds(dto.getRoleIds());

        boolean deactivating = Boolean.TRUE.equals(user.getActive()) && Boolean.FALSE.equals(dto.getActive());
        boolean losesAdmin = roles.stream().noneMatch(r -> RoleCodes.ADMIN.equals(r.getCode()));
        if (deactivating) {
            assertNotSelf(id, "desactivar su propio usuario");
        }
        if ((deactivating || losesAdmin) && isActiveAdmin(user)) {
            assertNotLastAdmin(id);
        }

        userRepository.save(userAppMapper.fromDto(dto, user));
        userRoleService.replaceRoles(user, roles);
        if (deactivating) {
            refreshTokenService.revokeAllForUser(user.getUsername());
        }
        log.info("Usuario actualizado: {} (activo={}, roles={})", user.getUsername(), dto.getActive(), roleCodes(roles));
        return getUserAppPojoById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void softDelete(Integer id) {
        UserApp user = getById(id);
        assertNotSelf(id, "desactivar su propio usuario");
        if (isActiveAdmin(user)) {
            assertNotLastAdmin(id);
        }
        user.setActive(false);
        userRepository.save(user);
        refreshTokenService.revokeAllForUser(user.getUsername());
        log.info("Usuario desactivado: {}", user.getUsername());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Integer id, String newPassword) {
        UserApp user = getById(id);
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        refreshTokenService.revokeAllForUser(user.getUsername());
        log.info("Contraseña restablecida para el usuario {}", user.getUsername());
    }

    private void attachRoles(List<UserAppPojo> users) {
        if (users.isEmpty()) {
            return;
        }
        Map<Integer, List<RolePojo>> rolesByUser = userRoleService
                .getRolesByUserIds(users.stream().map(UserAppPojo::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(RolePojo::getUserId));
        users.forEach(u -> u.setRoles(rolesByUser.getOrDefault(u.getId(), List.of())));
    }

    private void validateUnique(UserAppDto dto, Integer excludeId) {
        String username = dto.getUsername().trim();
        boolean usernameTaken = excludeId == null
                ? userRepository.existsByUsernameIgnoreCase(username)
                : userRepository.existsByUsernameIgnoreCaseAndIdNot(username, excludeId);
        if (usernameTaken) {
            throw new DuplicateException("Usuario", "Nombre de usuario", username);
        }
        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            String email = dto.getEmail().trim();
            boolean emailTaken = excludeId == null
                    ? userRepository.existsByEmailIgnoreCase(email)
                    : userRepository.existsByEmailIgnoreCaseAndIdNot(email, excludeId);
            if (emailTaken) {
                throw new DuplicateException("Usuario", "Correo", email);
            }
        }
    }

    private void assertNotSelf(Integer userId, String action) {
        if (Objects.equals(jwtService.getCurrentUserId(), userId)) {
            throw new NotAllowedException("No puede " + action + ".");
        }
    }

    private boolean isActiveAdmin(UserApp user) {
        return Boolean.TRUE.equals(user.getActive())
                && userRoleService.getRolesByUser(user).stream()
                    .anyMatch(ur -> RoleCodes.ADMIN.equals(ur.getRole().getCode()));
    }

    private void assertNotLastAdmin(Integer userId) {
        if (userRepository.countActiveUsersWithRoleExcluding(RoleCodes.ADMIN, userId) == 0) {
            throw new NotAllowedException("Debe existir al menos un usuario ADMIN activo.");
        }
    }

    private List<String> roleCodes(List<Role> roles) {
        return roles.stream().map(Role::getCode).toList();
    }
}
