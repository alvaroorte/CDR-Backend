package bo.cdr.app.appcatalog.data;

import bo.cdr.app.appbase.data.repository.GenericRepository;
import bo.cdr.app.appbase.presentation.response.SelectPojo;
import bo.cdr.app.appcatalog.domain.entity.Category;
import bo.cdr.app.appcatalog.presentation.response.pojo.CategoryPojo;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends GenericRepository<Category, Integer> {

    boolean existsByNameIgnoreCaseAndActiveIsTrue(String name);

    boolean existsByNameIgnoreCaseAndActiveIsTrueAndIdNot(String name, Integer id);

    /** Listado para la tabla de administración: sin imagen (se carga en el detalle). */
    @Query("""
        SELECT new bo.cdr.app.appcatalog.presentation.response.pojo.CategoryPojo(
            c.id, c.name, c.description, null, c.ranking, c.active, c.createdBy, c.updatedBy, c.createdAt, c.updatedAt)
        FROM Category c
        ORDER BY c.ranking, c.name
    """)
    List<CategoryPojo> findAllCategoryPojo();

    @Query("""
        SELECT new bo.cdr.app.appcatalog.presentation.response.pojo.CategoryPojo(
            c.id, c.name, c.description, c.imageBase64, c.ranking, c.active, c.createdBy, c.updatedBy, c.createdAt, c.updatedAt)
        FROM Category c WHERE c.id = :id
    """)
    Optional<CategoryPojo> getCategoryPojoById(@Param("id") Integer id);

    @Query("""
        SELECT new bo.cdr.app.appbase.presentation.response.SelectPojo(c.id, c.name)
        FROM Category c WHERE c.active = true
        ORDER BY c.ranking, c.name
    """)
    List<SelectPojo> findSelect();

    /** Catálogo del POS: activas, en el orden configurado, con imagen. */
    List<Category> findAllByActiveIsTrueOrderByRankingAscNameAsc();
}
