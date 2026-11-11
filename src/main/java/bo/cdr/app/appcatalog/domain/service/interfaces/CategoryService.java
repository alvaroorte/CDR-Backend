package bo.cdr.app.appcatalog.domain.service.interfaces;

import bo.cdr.app.appbase.domain.service.CRUDService;
import bo.cdr.app.appbase.presentation.response.SelectPojo;
import bo.cdr.app.appcatalog.domain.entity.Category;
import bo.cdr.app.appcatalog.presentation.request.dto.CategoryDto;
import bo.cdr.app.appcatalog.presentation.response.pojo.CategoryPojo;

import java.util.List;

public interface CategoryService extends CRUDService<Category, Integer> {
    Category create(CategoryDto dto);
    Category update(Integer id, CategoryDto dto);
    List<CategoryPojo> getAllCategoryPojo();
    CategoryPojo getCategoryPojoById(Integer id);
    List<SelectPojo> getSelect();
    /** Categoría existente y activa (para asignarla a un producto). */
    Category getActiveById(Integer id);
}
