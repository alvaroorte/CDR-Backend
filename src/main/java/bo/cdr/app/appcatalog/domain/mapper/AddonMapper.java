package bo.cdr.app.appcatalog.domain.mapper;

import bo.cdr.app.appcatalog.domain.entity.Addon;
import bo.cdr.app.appcatalog.presentation.request.dto.AddonDto;
import bo.cdr.app.common.MoneyUtil;
import org.springframework.stereotype.Component;

@Component
public class AddonMapper {
    public Addon fromDto(AddonDto dto, Addon addonFound) {
        Addon addon = addonFound != null ? addonFound : new Addon();
        addon.setName(dto.getName().trim());
        addon.setPrice(MoneyUtil.round(dto.getPrice()));
        addon.setActive(dto.getActive());
        return addon;
    }
}
