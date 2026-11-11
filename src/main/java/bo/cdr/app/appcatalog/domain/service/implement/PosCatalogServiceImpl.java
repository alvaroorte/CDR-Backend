package bo.cdr.app.appcatalog.domain.service.implement;

import bo.cdr.app.appcatalog.data.CategoryRepository;
import bo.cdr.app.appcatalog.data.PaymentMethodRepository;
import bo.cdr.app.appcatalog.data.ProductRepository;
import bo.cdr.app.appcatalog.domain.entity.Product;
import bo.cdr.app.appcatalog.domain.service.interfaces.ParameterService;
import bo.cdr.app.appcatalog.domain.service.interfaces.PosCatalogService;
import bo.cdr.app.appcatalog.presentation.response.pojo.PosCatalogPojo;
import bo.cdr.app.appcatalog.presentation.response.pojo.PosProductAddonProjection;
import bo.cdr.app.util.ImageBase64Util;
import bo.cdr.app.util.constants.ParameterCodes;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Catálogo del POS en una sola respuesta (decisión aprobada de la Etapa 3: incluye imágenes). */
@Service
@RequiredArgsConstructor
public class PosCatalogServiceImpl implements PosCatalogService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final ParameterService parameterService;

    @Override
    @Transactional(readOnly = true)
    public PosCatalogPojo getCatalog() {
        Map<Integer, List<PosCatalogPojo.Addon>> addonsByProduct = productRepository.findPosProductAddons().stream()
                .collect(Collectors.groupingBy(PosProductAddonProjection::productId,
                        Collectors.mapping(r -> new PosCatalogPojo.Addon(r.addonId(), r.addonName(), r.price()),
                                Collectors.toList())));

        Map<Integer, List<PosCatalogPojo.Product>> productsByCategory = productRepository.findPosProducts().stream()
                .collect(Collectors.groupingBy(p -> p.getCategory().getId(),
                        Collectors.mapping(p -> toProduct(p, addonsByProduct), Collectors.toList())));

        List<PosCatalogPojo.Category> categories = categoryRepository.findAllByActiveIsTrueOrderByRankingAscNameAsc()
                .stream()
                .filter(c -> productsByCategory.containsKey(c.getId()))
                .map(c -> new PosCatalogPojo.Category(c.getId(), c.getName(), image(c.getImageBase64()),
                        productsByCategory.get(c.getId())))
                .toList();

        List<PosCatalogPojo.PaymentMethod> paymentMethods = paymentMethodRepository.findAllByActiveIsTrueOrderByNameAsc()
                .stream()
                .map(m -> new PosCatalogPojo.PaymentMethod(m.getId(), m.getCode(), m.getName(),
                        Boolean.TRUE.equals(m.getRequiresReference()), Boolean.TRUE.equals(m.getGivesChange())))
                .toList();

        PosCatalogPojo.DiscountRules rules = new PosCatalogPojo.DiscountRules(
                parameterService.getDecimalValue(ParameterCodes.CASHIER_MAX_DISCOUNT_PERCENT),
                parameterService.getDecimalValue(ParameterCodes.DISCOUNT_REASON_REQUIRED_ABOVE_PERCENT));

        return new PosCatalogPojo(categories, paymentMethods, rules);
    }

    private PosCatalogPojo.Product toProduct(Product p, Map<Integer, List<PosCatalogPojo.Addon>> addonsByProduct) {
        return new PosCatalogPojo.Product(p.getId(), p.getCategory().getId(), p.getName(), p.getDescription(),
                image(p.getImageBase64()), p.getPrice(), Boolean.TRUE.equals(p.getAvailable()),
                addonsByProduct.getOrDefault(p.getId(), List.of()));
    }

    private String image(String base64) {
        return base64 == null || base64.isBlank() ? null : ImageBase64Util.stripDataUrlPrefix(base64);
    }
}
