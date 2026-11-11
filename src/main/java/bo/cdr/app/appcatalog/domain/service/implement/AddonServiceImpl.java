package bo.cdr.app.appcatalog.domain.service.implement;

import bo.cdr.app.appbase.data.repository.GenericRepository;
import bo.cdr.app.appbase.domain.service.CRUDServiceImpl;
import bo.cdr.app.appcatalog.data.AddonRepository;
import bo.cdr.app.appcatalog.domain.entity.Addon;
import bo.cdr.app.appcatalog.domain.mapper.AddonMapper;
import bo.cdr.app.appcatalog.domain.service.interfaces.AddonService;
import bo.cdr.app.appcatalog.presentation.request.dto.AddonDto;
import bo.cdr.app.appcatalog.presentation.response.pojo.AddonPojo;
import bo.cdr.app.exception.DuplicateException;
import bo.cdr.app.exception.EntityNotFoundException;
import bo.cdr.app.exception.response.ValidationErrorException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AddonServiceImpl extends CRUDServiceImpl<Addon, Integer> implements AddonService {

    private final AddonRepository addonRepository;
    private final AddonMapper addonMapper;

    @Override
    protected GenericRepository<Addon, Integer> getRepository() {
        return addonRepository;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Addon create(AddonDto dto) {
        validateDuplicate(dto, null);
        return addonRepository.save(addonMapper.fromDto(dto, null));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Addon update(Integer id, AddonDto dto) {
        Addon addonFound = getById(id);
        validateDuplicate(dto, id);
        return addonRepository.save(addonMapper.fromDto(dto, addonFound));
    }

    @Override
    public List<AddonPojo> getAllAddonPojo() {
        return addonRepository.findAllAddonPojo();
    }

    @Override
    public AddonPojo getAddonPojoById(Integer id) {
        return addonRepository.getAddonPojoById(id)
                .orElseThrow(() -> new EntityNotFoundException("Adicional", id));
    }

    @Override
    public List<AddonPojo> getActive() {
        return addonRepository.findActiveAddonPojo();
    }

    @Override
    public Set<Addon> getActiveByIds(Collection<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return new HashSet<>();
        }
        Set<Integer> uniqueIds = new HashSet<>(ids);
        List<Addon> found = addonRepository.findAllByIdInAndActiveIsTrue(uniqueIds);
        if (found.size() != uniqueIds.size()) {
            throw new ValidationErrorException("Uno o más adicionales seleccionados no existen o están inactivos.");
        }
        return new HashSet<>(found);
    }

    private void validateDuplicate(AddonDto dto, Integer excludeId) {
        if (!Boolean.TRUE.equals(dto.getActive())) {
            return;
        }
        String name = dto.getName().trim();
        boolean exists = excludeId == null
                ? addonRepository.existsByNameIgnoreCaseAndActiveIsTrue(name)
                : addonRepository.existsByNameIgnoreCaseAndActiveIsTrueAndIdNot(name, excludeId);
        if (exists) {
            throw new DuplicateException("Adicional", "Nombre", name);
        }
    }
}
