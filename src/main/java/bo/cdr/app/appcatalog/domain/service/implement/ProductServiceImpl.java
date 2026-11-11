package bo.cdr.app.appcatalog.domain.service.implement;

import bo.cdr.app.appbase.data.repository.GenericRepository;
import bo.cdr.app.appbase.domain.service.CRUDServiceImpl;
import bo.cdr.app.appbase.presentation.response.SelectPojo;
import bo.cdr.app.appcatalog.data.ProductRepository;
import bo.cdr.app.appcatalog.domain.entity.Addon;
import bo.cdr.app.appcatalog.domain.entity.Category;
import bo.cdr.app.appcatalog.domain.entity.Product;
import bo.cdr.app.appcatalog.domain.mapper.ProductMapper;
import bo.cdr.app.appcatalog.domain.service.interfaces.AddonService;
import bo.cdr.app.appcatalog.domain.service.interfaces.CategoryService;
import bo.cdr.app.appcatalog.domain.service.interfaces.ProductService;
import bo.cdr.app.appcatalog.presentation.request.dto.ProductDto;
import bo.cdr.app.appcatalog.presentation.response.pojo.ProductAddonProjection;
import bo.cdr.app.appcatalog.presentation.response.pojo.ProductPojo;
import bo.cdr.app.exception.DuplicateException;
import bo.cdr.app.exception.EntityNotFoundException;
import bo.cdr.app.appcatalog.domain.event.ProductAvailabilityChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl extends CRUDServiceImpl<Product, Integer> implements ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final CategoryService categoryService;
    private final AddonService addonService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    protected GenericRepository<Product, Integer> getRepository() {
        return productRepository;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductPojo create(ProductDto dto) {
        validateDuplicate(dto, null);
        Category category = categoryService.getActiveById(dto.getCategoryId());
        Set<Addon> addons = addonService.getActiveByIds(dto.getAddonIds());
        Product saved = productRepository.save(productMapper.fromDto(dto, null, category, addons));
        return getProductPojoById(saved.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductPojo update(Integer id, ProductDto dto) {
        Product productFound = getById(id);
        validateDuplicate(dto, id);
        Category category = categoryService.getActiveById(dto.getCategoryId());
        Set<Addon> addons = addonService.getActiveByIds(dto.getAddonIds());
        productRepository.saveAndFlush(productMapper.fromDto(dto, productFound, category, addons));
        return getProductPojoById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductPojo> getAllProductPojo() {
        List<ProductPojo> products = productRepository.findAllProductPojo();
        attachAddons(products);
        return products;
    }

    @Override
    @Transactional(readOnly = true)
    public ProductPojo getProductPojoById(Integer id) {
        ProductPojo product = productRepository.getProductPojoById(id)
                .orElseThrow(() -> new EntityNotFoundException("Producto", id));
        attachAddons(List.of(product));
        return product;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductPojo changeAvailability(Integer id, boolean available) {
        Product product = getById(id);
        product.setAvailable(available);
        productRepository.saveAndFlush(product);
        log.info("Producto {} marcado como {}", id, available ? "disponible" : "agotado");
        eventPublisher.publishEvent(new ProductAvailabilityChangedEvent(id, available, product.getVersion()));
        return getProductPojoById(id);
    }

    private void attachAddons(List<ProductPojo> products) {
        if (products.isEmpty()) {
            return;
        }
        Map<Integer, List<SelectPojo>> addonsByProduct = productRepository
                .findAddonsByProductIds(products.stream().map(ProductPojo::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(ProductAddonProjection::productId,
                        Collectors.mapping(row -> new SelectPojo(row.addonId(), row.addonName()), Collectors.toList())));
        products.forEach(p -> p.setAddons(addonsByProduct.getOrDefault(p.getId(), List.of())));
    }

    private void validateDuplicate(ProductDto dto, Integer excludeId) {
        if (!Boolean.TRUE.equals(dto.getActive())) {
            return;
        }
        String name = dto.getName().trim();
        boolean exists = excludeId == null
                ? productRepository.existsByNameIgnoreCaseAndActiveIsTrue(name)
                : productRepository.existsByNameIgnoreCaseAndActiveIsTrueAndIdNot(name, excludeId);
        if (exists) {
            throw new DuplicateException("Producto", "Nombre", name);
        }
    }
}
