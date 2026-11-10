package bo.cdr.app.appsecurity.domain.service;

import bo.cdr.app.appbase.data.repository.GenericRepository;
import bo.cdr.app.appbase.domain.service.CRUDServiceImpl;
import bo.cdr.app.appsecurity.auth.jwt.JwtService;
import bo.cdr.app.appsecurity.data.UserRepository;
import bo.cdr.app.appsecurity.domain.entity.UserApp;
import bo.cdr.app.appsecurity.presentation.response.pojo.UserInfoPojo;
import bo.cdr.app.exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends CRUDServiceImpl<UserApp, Integer> implements UserService {

    private final UserRepository userRepository;
    private final UserRoleService userRoleService;
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
}
