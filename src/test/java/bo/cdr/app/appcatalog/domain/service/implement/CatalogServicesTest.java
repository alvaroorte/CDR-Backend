package bo.cdr.app.appcatalog.domain.service.implement;

import bo.cdr.app.appcatalog.data.AddonRepository;
import bo.cdr.app.appcatalog.data.CategoryRepository;
import bo.cdr.app.appcatalog.data.ParameterRepository;
import bo.cdr.app.appcatalog.data.ProductRepository;
import bo.cdr.app.appcatalog.domain.entity.Addon;
import bo.cdr.app.appcatalog.domain.entity.Category;
import bo.cdr.app.appcatalog.domain.entity.Parameter;
import bo.cdr.app.appcatalog.domain.entity.Product;
import bo.cdr.app.appcatalog.domain.mapper.AddonMapper;
import bo.cdr.app.appcatalog.domain.mapper.CategoryMapper;
import bo.cdr.app.appcatalog.domain.mapper.ProductMapper;
import bo.cdr.app.appcatalog.domain.service.interfaces.AddonService;
import bo.cdr.app.appcatalog.domain.service.interfaces.CategoryService;
import bo.cdr.app.appcatalog.presentation.request.dto.ProductDto;
import bo.cdr.app.appcatalog.presentation.response.pojo.ParameterPojo;
import bo.cdr.app.appcatalog.presentation.response.pojo.ProductPojo;
import bo.cdr.app.common.enums.ParameterValueTypeEnum;
import bo.cdr.app.exception.DuplicateException;
import bo.cdr.app.exception.HasAssociatedEntityException;
import bo.cdr.app.exception.UpdateNotAllowedException;
import bo.cdr.app.exception.response.ValidationErrorException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CatalogServicesTest {

    @Nested
    class ProductServiceTest {
        @Mock ProductRepository productRepository;
        @Mock CategoryService categoryService;
        @Mock AddonService addonService;
        @Mock org.springframework.context.ApplicationEventPublisher eventPublisher;

        ProductServiceImpl service() {
            return new ProductServiceImpl(productRepository, new ProductMapper(), categoryService, addonService, eventPublisher);
        }

        private ProductDto dto() {
            ProductDto dto = new ProductDto();
            dto.setCategoryId(1);
            dto.setName(" Capuchino ");
            dto.setPrice(new BigDecimal("15.5"));
            dto.setAvailable(true);
            dto.setActive(true);
            dto.setAddonIds(List.of(3));
            return dto;
        }

        @Test
        void create_roundsPrice_trimsName_andLinksCategoryAndAddons() {
            Category category = new Category();
            Addon addon = new Addon();
            when(categoryService.getActiveById(1)).thenReturn(category);
            when(addonService.getActiveByIds(List.of(3))).thenReturn(Set.of(addon));
            when(productRepository.save(any(Product.class))).thenAnswer(inv -> { Product p = inv.getArgument(0); p.setId(9); return p; });
            when(productRepository.getProductPojoById(9)).thenReturn(Optional.of(new ProductPojo()));

            service().create(dto());

            ArgumentCaptor<Product> saved = ArgumentCaptor.forClass(Product.class);
            verify(productRepository).save(saved.capture());
            assertThat(saved.getValue().getName()).isEqualTo("Capuchino");
            assertThat(saved.getValue().getPrice()).isEqualByComparingTo("15.50");
            assertThat(saved.getValue().getPrice().scale()).isEqualTo(2);
            assertThat(saved.getValue().getCategory()).isSameAs(category);
            assertThat(saved.getValue().getAddons()).containsExactly(addon);
        }

        @Test
        void create_rejectsDuplicateActiveName() {
            when(productRepository.existsByNameIgnoreCaseAndActiveIsTrue("Capuchino")).thenReturn(true);
            assertThatThrownBy(() -> service().create(dto())).isInstanceOf(DuplicateException.class);
            verifyNoInteractions(categoryService, addonService);
        }

        @Test
        void create_rejectsInactiveCategory() {
            when(categoryService.getActiveById(1)).thenThrow(new ValidationErrorException("La categoría seleccionada está inactiva."));
            assertThatThrownBy(() -> service().create(dto())).isInstanceOf(ValidationErrorException.class);
            verify(productRepository, never()).save(any());
        }

        @Test
        void changeAvailability_onlyTouchesAvailableFlag() {
            Product product = new Product();
            product.setId(9);
            product.setPrice(new BigDecimal("15.50"));
            when(productRepository.findById(9)).thenReturn(Optional.of(product));
            when(productRepository.getProductPojoById(9)).thenReturn(Optional.of(new ProductPojo()));

            service().changeAvailability(9, false);

            assertThat(product.getAvailable()).isFalse();
            assertThat(product.getPrice()).isEqualByComparingTo("15.50");
            // Se publica para los POS abiertos (CatalogEventPublisher lo envía tras el commit)
            verify(eventPublisher).publishEvent(new bo.cdr.app.appcatalog.domain.event.ProductAvailabilityChangedEvent(9, false, null));
        }
    }

    @Nested
    class CategoryServiceTest {
        @Mock CategoryRepository categoryRepository;

        @Test
        void softDelete_rejectsCategoryWithActiveProducts() {
            Category category = new Category();
            Product active = new Product();
            active.setActive(true);
            category.getProducts().add(active);
            when(categoryRepository.findById(1)).thenReturn(Optional.of(category));

            CategoryServiceImpl service = new CategoryServiceImpl(categoryRepository, new CategoryMapper());

            assertThatThrownBy(() -> service.softDelete(1)).isInstanceOf(HasAssociatedEntityException.class);
            verify(categoryRepository, never()).save(any());
        }

        @Test
        void getActiveById_rejectsInactiveCategory() {
            Category category = new Category();
            category.setActive(false);
            when(categoryRepository.findById(1)).thenReturn(Optional.of(category));

            CategoryServiceImpl service = new CategoryServiceImpl(categoryRepository, new CategoryMapper());

            assertThatThrownBy(() -> service.getActiveById(1)).isInstanceOf(ValidationErrorException.class);
        }
    }

    @Nested
    class AddonServiceTest {
        @Mock AddonRepository addonRepository;

        @Test
        void getActiveByIds_failsWhenSomeAddonIsInactiveOrMissing() {
            when(addonRepository.findAllByIdInAndActiveIsTrue(Set.of(1, 2))).thenReturn(List.of(new Addon()));
            AddonServiceImpl service = new AddonServiceImpl(addonRepository, new AddonMapper());

            assertThatThrownBy(() -> service.getActiveByIds(List.of(1, 2))).isInstanceOf(ValidationErrorException.class);
        }

        @Test
        void getActiveByIds_returnsEmpty_whenNoIds() {
            AddonServiceImpl service = new AddonServiceImpl(addonRepository, new AddonMapper());
            assertThat(service.getActiveByIds(List.of())).isEmpty();
            verifyNoInteractions(addonRepository);
        }
    }

    @Nested
    class ParameterServiceTest {
        @Mock ParameterRepository parameterRepository;

        private Parameter parameter(ParameterValueTypeEnum type, boolean editable) {
            Parameter p = new Parameter();
            p.setId(1);
            p.setCode("CASHIER_MAX_DISCOUNT_PERCENT");
            p.setName("Descuento máximo");
            p.setValue("50");
            p.setValueType(type);
            p.setEditable(editable);
            return p;
        }

        @Test
        void updateValue_rejectsPercentageAbove100() {
            when(parameterRepository.findById(1)).thenReturn(Optional.of(parameter(ParameterValueTypeEnum.PERCENTAGE, true)));
            ParameterServiceImpl service = new ParameterServiceImpl(parameterRepository);

            assertThatThrownBy(() -> service.updateValue(1, "150")).isInstanceOf(ValidationErrorException.class);
        }

        @Test
        void updateValue_rejectsNonNumericPercentage() {
            when(parameterRepository.findById(1)).thenReturn(Optional.of(parameter(ParameterValueTypeEnum.PERCENTAGE, true)));
            ParameterServiceImpl service = new ParameterServiceImpl(parameterRepository);

            assertThatThrownBy(() -> service.updateValue(1, "mucho")).isInstanceOf(ValidationErrorException.class);
        }

        @Test
        void updateValue_normalizesDecimalComma() {
            Parameter p = parameter(ParameterValueTypeEnum.PERCENTAGE, true);
            when(parameterRepository.findById(1)).thenReturn(Optional.of(p));
            when(parameterRepository.getParameterPojoById(1)).thenReturn(Optional.of(new ParameterPojo()));
            ParameterServiceImpl service = new ParameterServiceImpl(parameterRepository);

            service.updateValue(1, "12,50");

            assertThat(p.getValue()).isEqualTo("12.5");
        }

        @Test
        void updateValue_rejectsNonEditableParameter() {
            when(parameterRepository.findById(1)).thenReturn(Optional.of(parameter(ParameterValueTypeEnum.TEXT, false)));
            ParameterServiceImpl service = new ParameterServiceImpl(parameterRepository);

            assertThatThrownBy(() -> service.updateValue(1, "x")).isInstanceOf(UpdateNotAllowedException.class);
        }
    }
}
