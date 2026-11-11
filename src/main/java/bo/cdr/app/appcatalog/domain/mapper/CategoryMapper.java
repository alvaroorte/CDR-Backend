package bo.cdr.app.appcatalog.domain.mapper;

import bo.cdr.app.appcatalog.domain.entity.Category;
import bo.cdr.app.appcatalog.presentation.request.dto.CategoryDto;
import bo.cdr.app.util.ImageBase64Util;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {
    public Category fromDto(CategoryDto dto, Category categoryFound) {
        Category category = categoryFound != null ? categoryFound : new Category();
        category.setName(dto.getName().trim());
        category.setDescription(dto.getDescription());
        category.setImageBase64(ImageBase64Util.stripDataUrlPrefix(dto.getImageBase64()));
        category.setRanking(dto.getRanking());
        category.setActive(dto.getActive());
        return category;
    }
}
