package bo.cdr.app.appbase.domain.mapper;

import bo.cdr.app.appbase.presentation.response.PagePojo;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
public class PagePojoMapper {
    public <T> PagePojo<T> convertToPagePojo(Page<T> page) {
        PagePojo<T> dto = new PagePojo<>();
        dto.setContent(page.getContent());
        dto.setLast(page.isLast());
        dto.setPageNumber(page.getNumber());
        dto.setPageSize(page.getSize());
        dto.setTotalPages(page.getTotalPages());
        dto.setTotalElements(page.getTotalElements());
        return dto;
    }
}