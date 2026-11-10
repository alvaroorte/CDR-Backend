package bo.cdr.app.appsecurity.domain.service;

import bo.cdr.app.appbase.data.repository.GenericRepository;
import bo.cdr.app.appbase.domain.service.CRUDServiceImpl;
import bo.cdr.app.appsecurity.data.PermissionRepository;
import bo.cdr.app.appsecurity.domain.entity.Permission;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PermissionServiceImpl extends CRUDServiceImpl<Permission, Integer> implements PermissionService {

    private final PermissionRepository permissionRepository;

    @Override
    protected GenericRepository<Permission, Integer> getRepository() {
        return permissionRepository;
    }

    @Override
    public boolean existsByCode(String code) {
        return permissionRepository.existsByCode(code);
    }
}
