package bo.cdr.app.appcatalog.data;

import bo.cdr.app.appbase.data.repository.GenericRepository;
import bo.cdr.app.appcatalog.domain.entity.Product;
import bo.cdr.app.appcatalog.presentation.response.pojo.ProductAddonProjection;
import bo.cdr.app.appcatalog.presentation.response.pojo.PosProductAddonProjection;
import bo.cdr.app.appcatalog.presentation.response.pojo.ProductPojo;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends GenericRepository<Product, Integer> {

    boolean existsByNameIgnoreCaseAndActiveIsTrue(String name);

    boolean existsByNameIgnoreCaseAndActiveIsTrueAndIdNot(String name, Integer id);

    @Query("""
        SELECT new bo.cdr.app.appcatalog.presentation.response.pojo.ProductPojo(
            p.id, c.id, c.name, p.name, p.description, null, p.price, p.available, p.version, p.active,
            p.createdBy, p.updatedBy, p.createdAt, p.updatedAt)
        FROM Product p JOIN p.category c
        ORDER BY c.ranking, c.name, p.name
    """)
    List<ProductPojo> findAllProductPojo();

    @Query("""
        SELECT new bo.cdr.app.appcatalog.presentation.response.pojo.ProductPojo(
            p.id, c.id, c.name, p.name, p.description, p.imageBase64, p.price, p.available, p.version, p.active,
            p.createdBy, p.updatedBy, p.createdAt, p.updatedAt)
        FROM Product p JOIN p.category c
        WHERE p.id = :id
    """)
    Optional<ProductPojo> getProductPojoById(@Param("id") Integer id);

    @Query("""
        SELECT new bo.cdr.app.appcatalog.presentation.response.pojo.ProductAddonProjection(p.id, a.id, a.name)
        FROM Product p JOIN p.addons a
        WHERE p.id IN :productIds
        ORDER BY a.name
    """)
    List<ProductAddonProjection> findAddonsByProductIds(@Param("productIds") Collection<Integer> productIds);

    /** Catálogo del POS: productos activos de categorías activas (incluye agotados). */
    @Query("""
        SELECT p FROM Product p JOIN FETCH p.category c
        WHERE p.active = true AND c.active = true
        ORDER BY p.name
    """)
    List<Product> findPosProducts();

    @Query("""
        SELECT new bo.cdr.app.appcatalog.presentation.response.pojo.PosProductAddonProjection(p.id, a.id, a.name, a.price)
        FROM Product p JOIN p.addons a
        WHERE p.active = true AND a.active = true
        ORDER BY a.name
    """)
    List<PosProductAddonProjection> findPosProductAddons();
}
