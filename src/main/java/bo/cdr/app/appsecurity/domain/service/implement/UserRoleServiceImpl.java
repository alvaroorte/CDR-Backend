package bo.cdr.app.appsecurity.domain.service.implement;


import bo.cdr.app.appsecurity.domain.service.interfaces.*;
import bo.cdr.app.appbase.data.repository.GenericRepository;
import bo.cdr.app.appbase.domain.service.CRUDServiceImpl;
import bo.cdr.app.appsecurity.domain.entity.Role;
import bo.cdr.app.appsecurity.domain.entity.UserApp;
import bo.cdr.app.appsecurity.domain.entity.UserRole;
import bo.cdr.app.appsecurity.presentation.response.pojo.RolePojo;
import bo.cdr.app.appsecurity.data.UserRoleRepository;
import bo.cdr.app.exception.DuplicateException;
import bo.cdr.app.exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;


@Service
@RequiredArgsConstructor
public class UserRoleServiceImpl extends CRUDServiceImpl<UserRole, Integer> implements UserRoleService {

    private final UserRoleRepository userRoleRepository;

    @Override
    protected GenericRepository<UserRole, Integer> getRepository() {
        return userRoleRepository;
    }

    @Override
    public List<RolePojo> getRolesByUserId(Integer userId) {
        return userRoleRepository.getRolesByUserId(userId);
    }

    @Override
    public List<RolePojo> getRolesByUserIds(Collection<Integer> userIds) {
        return userRoleRepository.getRolesByUserIds(userIds);
    }

    @Override
    public List<UserRole> getRolesByUser(UserApp user) {
        return userRoleRepository.findByUser(user);
    }

    @Override
    public boolean existsByUserAndRole(UserApp user, Role role) {
        return userRoleRepository.existsByUserAndRole(user, role);
    }

    @Override
    public UserRole create(UserApp user, Role role) {
        if (userRoleRepository.existsByUserAndRole(user, role)) {
            throw new DuplicateException("El usuario ya tiene asignado este rol");
        }

        UserRole userRole = new UserRole();
        userRole.setUser(user);
        userRole.setRole(role);

        return userRoleRepository.save(userRole);
    }
    @Override
    public void deleteUserRole(UserApp user, Role role) {
        UserRole userRole = userRoleRepository.findByUserAndRole(user, role)
                .orElseThrow(() -> new EntityNotFoundException("El usuario no tiene asignado este rol"));

        this.delete(userRole.getId());
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public void replaceRoles(UserApp user, java.util.Collection<Role> roles) {
        userRoleRepository.deleteByUserId(user.getId());
        roles.forEach(role -> {
            UserRole userRole = new UserRole();
            userRole.setUser(user);
            userRole.setRole(role);
            userRoleRepository.save(userRole);
        });
    }

    @Override
    public boolean isAssignedToActiveUsers(Integer roleId) {
        return userRoleRepository.existsByRoleIdAndUserActiveIsTrue(roleId);
    }
}
