package bo.cdr.app.appcatalog.domain.mapper;

import bo.cdr.app.appcatalog.domain.entity.Addon;
import bo.cdr.app.appcatalog.domain.entity.Category;
import bo.cdr.app.appcatalog.domain.entity.Product;
import bo.cdr.app.appcatalog.presentation.request.dto.ProductDto;
import bo.cdr.app.common.MoneyUtil;
import bo.cdr.app.util.ImageBase64Util;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class ProductMapper {
    public Product fromDto(ProductDto dto, Product productFound, Category category, Set<Addon> addons) {
        Product product = productFound != null ? productFound : new Product();
        product.setCategory(category);
        product.setName(dto.getName().trim());
        product.setDescription(dto.getDescription());
        product.setImageBase64(ImageBase64Util.stripDataUrlPrefix(dto.getImageBase64()));
        product.setPrice(MoneyUtil.round(dto.getPrice()));
        product.setAvailable(dto.getAvailable());
        product.setActive(dto.getActive());
        product.getAddons().clear();
        product.getAddons().addAll(addons);
        return product;
    }
}
