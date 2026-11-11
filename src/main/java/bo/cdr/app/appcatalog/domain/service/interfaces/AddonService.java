package bo.cdr.app.appcatalog.domain.service.interfaces;

import bo.cdr.app.appbase.domain.service.CRUDService;
import bo.cdr.app.appcatalog.domain.entity.Addon;
import bo.cdr.app.appcatalog.presentation.request.dto.AddonDto;
import bo.cdr.app.appcatalog.presentation.response.pojo.AddonPojo;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface AddonService extends CRUDService<Addon, Integer> {
    Addon create(AddonDto dto);
    Addon update(Integer id, AddonDto dto);
    List<AddonPojo> getAllAddonPojo();
    AddonPojo getAddonPojoById(Integer id);
    List<AddonPojo> getActive();
    /** Adicionales activos con esos ids; falla si alguno no existe o está inactivo. */
    Set<Addon> getActiveByIds(Collection<Integer> ids);
}
