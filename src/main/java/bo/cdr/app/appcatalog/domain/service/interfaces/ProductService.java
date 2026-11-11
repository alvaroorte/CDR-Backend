package bo.cdr.app.appcatalog.domain.service.interfaces;

import bo.cdr.app.appbase.domain.service.CRUDService;
import bo.cdr.app.appcatalog.domain.entity.Product;
import bo.cdr.app.appcatalog.presentation.request.dto.ProductDto;
import bo.cdr.app.appcatalog.presentation.response.pojo.ProductPojo;

import java.util.List;

public interface ProductService extends CRUDService<Product, Integer> {
    ProductPojo create(ProductDto dto);
    ProductPojo update(Integer id, ProductDto dto);
    List<ProductPojo> getAllProductPojo();
    ProductPojo getProductPojoById(Integer id);
    /** Agotado temporalmente / disponible. Permitido a ADMIN y CAJERO (permiso por URL). */
    ProductPojo changeAvailability(Integer id, boolean available);
}
