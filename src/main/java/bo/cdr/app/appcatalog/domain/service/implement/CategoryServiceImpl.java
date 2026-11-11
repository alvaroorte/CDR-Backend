package bo.cdr.app.appcatalog.domain.service.implement;

import bo.cdr.app.appbase.data.repository.GenericRepository;
import bo.cdr.app.appbase.domain.service.CRUDServiceImpl;
import bo.cdr.app.appbase.presentation.response.SelectPojo;
import bo.cdr.app.appcatalog.data.CategoryRepository;
import bo.cdr.app.appcatalog.domain.entity.Category;
import bo.cdr.app.appcatalog.domain.mapper.CategoryMapper;
import bo.cdr.app.appcatalog.domain.service.interfaces.CategoryService;
import bo.cdr.app.appcatalog.presentation.request.dto.CategoryDto;
import bo.cdr.app.appcatalog.presentation.response.pojo.CategoryPojo;
import bo.cdr.app.exception.DuplicateException;
import bo.cdr.app.exception.EntityNotFoundException;
import bo.cdr.app.exception.response.ValidationErrorException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl extends CRUDServiceImpl<Category, Integer> implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Override
    protected GenericRepository<Category, Integer> getRepository() {
        return categoryRepository;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Category create(CategoryDto dto) {
        validateDuplicate(dto, null);
        return categoryRepository.save(categoryMapper.fromDto(dto, null));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Category update(Integer id, CategoryDto dto) {
        Category categoryFound = getById(id);
        if (Boolean.TRUE.equals(categoryFound.getActive()) && Boolean.FALSE.equals(dto.getActive())) {
            checkIfElementsExist(categoryFound.hasDependencies());
        }
        validateDuplicate(dto, id);
        return categoryRepository.save(categoryMapper.fromDto(dto, categoryFound));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void softDelete(Integer id) {
        Category category = getById(id);
        checkIfElementsExist(category.hasDependencies());
        category.setActive(false);
        categoryRepository.save(category);
    }

    @Override
    public List<CategoryPojo> getAllCategoryPojo() {
        return categoryRepository.findAllCategoryPojo();
    }

    @Override
    public CategoryPojo getCategoryPojoById(Integer id) {
        return categoryRepository.getCategoryPojoById(id)
                .orElseThrow(() -> new EntityNotFoundException("Categoría", id));
    }

    @Override
    public List<SelectPojo> getSelect() {
        return categoryRepository.findSelect();
    }

    @Override
    public Category getActiveById(Integer id) {
        Category category = getById(id);
        if (!Boolean.TRUE.equals(category.getActive())) {
            throw new ValidationErrorException("La categoría seleccionada está inactiva.");
        }
        return category;
    }

    private void validateDuplicate(CategoryDto dto, Integer excludeId) {
        if (!Boolean.TRUE.equals(dto.getActive())) {
            return; // la unicidad solo aplica entre activos (índice parcial ux_categories_name)
        }
        String name = dto.getName().trim();
        boolean exists = excludeId == null
                ? categoryRepository.existsByNameIgnoreCaseAndActiveIsTrue(name)
                : categoryRepository.existsByNameIgnoreCaseAndActiveIsTrueAndIdNot(name, excludeId);
        if (exists) {
            throw new DuplicateException("Categoría", "Nombre", name);
        }
    }
}
