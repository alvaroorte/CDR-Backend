package bo.cdr.app.appsecurity.domain.service.implement;

import bo.cdr.app.appbase.data.repository.GenericRepository;
import bo.cdr.app.appbase.domain.service.CRUDServiceImpl;
import bo.cdr.app.appsecurity.data.PermissionRepository;
import bo.cdr.app.appsecurity.domain.entity.Permission;
import bo.cdr.app.appsecurity.domain.service.interfaces.PermissionService;
import bo.cdr.app.appsecurity.presentation.response.pojo.PermissionPojo;
import bo.cdr.app.exception.response.ValidationErrorException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    @Override
    public List<PermissionPojo> getAllActive() {
        return permissionRepository.findAllActivePermissionPojo();
    }

    @Override
    public List<Permission> getActiveByIds(Collection<Integer> ids) {
        Set<Integer> uniqueIds = new HashSet<>(ids);
        if (uniqueIds.isEmpty()) {
            return List.of();
        }
        List<Permission> found = permissionRepository.findAllByIdInAndActiveIsTrue(uniqueIds);
        if (found.size() != uniqueIds.size()) {
            throw new ValidationErrorException("Uno o más permisos seleccionados no existen o están inactivos.");
        }
        return found;
    }
}
