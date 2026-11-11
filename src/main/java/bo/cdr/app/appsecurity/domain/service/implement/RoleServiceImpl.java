package bo.cdr.app.appsecurity.domain.service.implement;

import bo.cdr.app.appbase.data.repository.GenericRepository;
import bo.cdr.app.appbase.domain.service.CRUDServiceImpl;
import bo.cdr.app.appsecurity.data.RolePermissionRepository;
import bo.cdr.app.appsecurity.data.RoleRepository;
import bo.cdr.app.appsecurity.domain.entity.Permission;
import bo.cdr.app.appsecurity.domain.entity.Role;
import bo.cdr.app.appsecurity.domain.entity.RolePermission;
import bo.cdr.app.appsecurity.domain.mapper.RoleMapper;
import bo.cdr.app.appsecurity.domain.service.interfaces.PermissionService;
import bo.cdr.app.appsecurity.domain.service.interfaces.RoleService;
import bo.cdr.app.appsecurity.domain.service.interfaces.UserRoleService;
import bo.cdr.app.appsecurity.presentation.request.dto.RoleDto;
import bo.cdr.app.appsecurity.presentation.response.pojo.RolePojo;
import bo.cdr.app.exception.DuplicateException;
import bo.cdr.app.exception.EntityNotFoundException;
import bo.cdr.app.exception.HasAssociatedEntityException;
import bo.cdr.app.exception.NotAllowedException;
import bo.cdr.app.exception.response.ValidationErrorException;
import bo.cdr.app.util.constants.RoleCodes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Roles y su asignación de permisos. Los roles del sistema (ADMIN, CAJERO, COCINA) no se
 * desactivan ni cambian de código porque el backend aplica reglas por esos códigos.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleServiceImpl extends CRUDServiceImpl<Role, Integer> implements RoleService {

    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final RoleMapper roleMapper;
    private final PermissionService permissionService;
    private final UserRoleService userRoleService;

    @Override
    protected GenericRepository<Role, Integer> getRepository() {
        return roleRepository;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RolePojo create(RoleDto dto) {
        String code = dto.getCode().trim();
        if (roleRepository.existsByCodeIgnoreCase(code)) {
            throw new DuplicateException("Rol", "Código", code.toUpperCase());
        }
        if (roleRepository.existsByNameIgnoreCase(dto.getName().trim())) {
            throw new DuplicateException("Rol", "Nombre", dto.getName().trim());
        }
        Role saved = roleRepository.save(roleMapper.fromDto(dto, null));
        return getRolePojoById(saved.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RolePojo update(Integer id, RoleDto dto) {
        Role found = getById(id);
        if (roleRepository.existsByNameIgnoreCaseAndIdNot(dto.getName().trim(), id)) {
            throw new DuplicateException("Rol", "Nombre", dto.getName().trim());
        }
        if (Boolean.FALSE.equals(dto.getActive()) && Boolean.TRUE.equals(found.getActive())) {
            assertCanDeactivate(found);
        }
        roleRepository.save(roleMapper.fromDto(dto, found));
        return getRolePojoById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void softDelete(Integer id) {
        Role role = getById(id);
        assertCanDeactivate(role);
        role.setActive(false);
        roleRepository.save(role);
    }

    @Override
    public List<RolePojo> getAllRolePojo() {
        List<RolePojo> roles = roleRepository.findAllRolePojo();
        roles.forEach(this::markSystem);
        return roles;
    }

    @Override
    public RolePojo getRolePojoById(Integer id) {
        RolePojo role = roleRepository.getRolePojoById(id)
                .orElseThrow(() -> new EntityNotFoundException("Rol", id));
        markSystem(role);
        return role;
    }

    @Override
    public List<RolePojo> getSelect() {
        return roleRepository.findSelect();
    }

    @Override
    public List<Role> getActiveByIds(Collection<Integer> ids) {
        Set<Integer> uniqueIds = new HashSet<>(ids);
        List<Role> found = roleRepository.findAllByIdInAndActiveIsTrue(uniqueIds);
        if (found.size() != uniqueIds.size()) {
            throw new ValidationErrorException("Uno o más roles seleccionados no existen o están inactivos.");
        }
        return found;
    }

    @Override
    public List<Integer> getPermissionIds(Integer roleId) {
        getById(roleId);
        return rolePermissionRepository.findPermissionIdsByRoleId(roleId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Integer> replacePermissions(Integer roleId, Collection<Integer> permissionIds) {
        Role role = getById(roleId);
        List<Permission> permissions = permissionService.getActiveByIds(permissionIds);
        rolePermissionRepository.deleteByRoleId(roleId);
        permissions.forEach(permission -> {
            RolePermission rolePermission = new RolePermission();
            rolePermission.setRole(role);
            rolePermission.setPermission(permission);
            rolePermissionRepository.save(rolePermission);
        });
        log.info("Permisos del rol {} reemplazados ({} permisos)", role.getCode(), permissions.size());
        return rolePermissionRepository.findPermissionIdsByRoleId(roleId);
    }

    private void assertCanDeactivate(Role role) {
        if (RoleCodes.SYSTEM_ROLES.contains(role.getCode())) {
            throw new NotAllowedException("El rol " + role.getName() + " es del sistema y no se puede desactivar.");
        }
        if (userRoleService.isAssignedToActiveUsers(role.getId())) {
            throw new HasAssociatedEntityException("el rol " + role.getName(), "usuarios activos");
        }
    }

    private void markSystem(RolePojo role) {
        role.setSystem(RoleCodes.SYSTEM_ROLES.contains(role.getCode()));
    }
}
