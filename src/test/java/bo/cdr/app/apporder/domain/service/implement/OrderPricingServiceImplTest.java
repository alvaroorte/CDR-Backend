package bo.cdr.app.apporder.domain.service.implement;

import bo.cdr.app.appcatalog.domain.entity.Addon;
import bo.cdr.app.appcatalog.domain.entity.Category;
import bo.cdr.app.appcatalog.domain.entity.Product;
import bo.cdr.app.appcatalog.domain.service.interfaces.ParameterService;
import bo.cdr.app.appcatalog.domain.service.interfaces.ProductService;
import bo.cdr.app.apporder.domain.model.PricedOrder;
import bo.cdr.app.apporder.presentation.request.dto.DiscountDto;
import bo.cdr.app.apporder.presentation.request.dto.OrderItemDto;
import bo.cdr.app.apporder.presentation.request.dto.OrderQuoteDto;
import bo.cdr.app.appsecurity.auth.jwt.JwtService;
import bo.cdr.app.common.enums.DiscountTypeEnum;
import bo.cdr.app.exception.NotAllowedException;
import bo.cdr.app.exception.response.ValidationErrorException;
import bo.cdr.app.util.constants.ParameterCodes;
import bo.cdr.app.util.constants.RoleCodes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderPricingServiceImplTest {

    @Mock ProductService productService;
    @Mock ParameterService parameterService;
    @Mock JwtService jwtService;

    OrderPricingServiceImpl service;
    Category bebidas;
    Addon almendra;
    Product capuchino;

    @BeforeEach
    void setUp() {
        service = new OrderPricingServiceImpl(productService, parameterService, jwtService);
        bebidas = new Category();
        bebidas.setId(1);
        bebidas.setName("Bebidas");
        almendra = new Addon();
        almendra.setId(5);
        almendra.setName("Leche de almendra");
        almendra.setPrice(new BigDecimal("3.50"));
        capuchino = product(10, "Capuchino", "15.00");
        capuchino.setAddons(Set.of(almendra));
        when(productService.getById(10)).thenReturn(capuchino);
        when(parameterService.getDecimalValue(ParameterCodes.CASHIER_MAX_DISCOUNT_PERCENT)).thenReturn(new BigDecimal("50"));
        when(parameterService.getDecimalValue(ParameterCodes.DISCOUNT_REASON_REQUIRED_ABOVE_PERCENT)).thenReturn(BigDecimal.ZERO);
        asRoles(RoleCodes.CAJERO);
    }

    private Product product(int id, String name, String price) {
        Product p = new Product();
        p.setId(id);
        p.setName(name);
        p.setPrice(new BigDecimal(price));
        p.setCategory(bebidas);
        return p;
    }

    private void asRoles(String... roles) {
        when(jwtService.getCurrentAuthorities()).thenReturn(List.of(roles));
    }

    private static OrderItemDto item(int productId, int quantity, Integer... addonIds) {
        OrderItemDto dto = new OrderItemDto();
        dto.setProductId(productId);
        dto.setQuantity(quantity);
        dto.setAddonIds(List.of(addonIds));
        return dto;
    }

    private static OrderQuoteDto quote(DiscountDto discount, OrderItemDto... items) {
        OrderQuoteDto dto = new OrderQuoteDto();
        dto.setItems(List.of(items));
        dto.setDiscount(discount);
        return dto;
    }

    private static DiscountDto discount(String type, String value, String reason) {
        DiscountDto dto = new DiscountDto();
        dto.setType(type);
        dto.setValue(new BigDecimal(value));
        dto.setReason(reason);
        return dto;
    }

    @Test
    void price_computesLineWithAddons_andIgnoresNothingFromClient() {
        PricedOrder priced = service.price(quote(null, item(10, 2, 5)));

        PricedOrder.Line line = priced.lines().getFirst();
        assertThat(line.unitPrice()).isEqualByComparingTo("15.00");
        assertThat(line.addonsUnitTotal()).isEqualByComparingTo("3.50");
        assertThat(line.lineSubtotal()).isEqualByComparingTo("37.00");
        assertThat(line.categoryName()).isEqualTo("Bebidas");
        assertThat(priced.subtotal()).isEqualByComparingTo("37.00");
        assertThat(priced.discountAmount()).isEqualByComparingTo("0");
        assertThat(priced.total()).isEqualByComparingTo("37.00");
        assertThat(priced.hasDiscount()).isFalse();
    }

    @Test
    void price_roundsPercentageDiscountHalfUp() {
        // 3 × 15.00 = 45.00; 12.5 % = 5.625 → 5.63
        PricedOrder priced = service.price(quote(discount("PERCENTAGE", "12.5", "Cliente frecuente"), item(10, 3)));

        assertThat(priced.discountType()).isEqualTo(DiscountTypeEnum.PERCENTAGE);
        assertThat(priced.discountAmount()).isEqualByComparingTo("5.63");
        assertThat(priced.discountAmount().scale()).isEqualTo(2);
        assertThat(priced.total()).isEqualByComparingTo("39.37");
        assertThat(priced.discountPercent()).isEqualByComparingTo("12.51");
    }

    @Test
    void price_appliesFixedAmountDiscount() {
        PricedOrder priced = service.price(quote(discount("FIXED_AMOUNT", "5", "Cortesía"), item(10, 2)));
        assertThat(priced.discountAmount()).isEqualByComparingTo("5.00");
        assertThat(priced.total()).isEqualByComparingTo("25.00");
        assertThat(priced.discountReason()).isEqualTo("Cortesía");
    }

    @Test
    void price_rejectsCashierDiscountAboveMax() {
        assertThatThrownBy(() -> service.price(quote(discount("PERCENTAGE", "60", "x"), item(10, 1))))
                .isInstanceOf(NotAllowedException.class)
                .hasMessageContaining("50");
    }

    @Test
    void price_comparesCashierFixedDiscountAsPercent() {
        // 10 sobre 15 = 66.67 % > 50 %
        assertThatThrownBy(() -> service.price(quote(discount("FIXED_AMOUNT", "10", "x"), item(10, 1))))
                .isInstanceOf(NotAllowedException.class);
    }

    @Test
    void price_allowsCashierExactlyAtMax() {
        PricedOrder priced = service.price(quote(discount("PERCENTAGE", "50", "Promo"), item(10, 1)));
        assertThat(priced.total()).isEqualByComparingTo("7.50");
    }

    @Test
    void price_allowsAdminAboveCashierMax() {
        asRoles(RoleCodes.ADMIN);
        PricedOrder priced = service.price(quote(discount("PERCENTAGE", "90", "Evento"), item(10, 1)));
        assertThat(priced.total()).isEqualByComparingTo("1.50");
    }

    @Test
    void price_rejectsDiscountForKitchen() {
        asRoles(RoleCodes.COCINA);
        assertThatThrownBy(() -> service.price(quote(discount("PERCENTAGE", "5", "x"), item(10, 1))))
                .isInstanceOf(NotAllowedException.class);
    }

    @Test
    void price_requiresReason_whenAboveThreshold() {
        assertThatThrownBy(() -> service.price(quote(discount("PERCENTAGE", "10", "  "), item(10, 1))))
                .isInstanceOf(ValidationErrorException.class)
                .hasMessageContaining("motivo");
    }

    @Test
    void price_rejectsTotalZero_evenForAdmin() {
        asRoles(RoleCodes.ADMIN);
        assertThatThrownBy(() -> service.price(quote(discount("PERCENTAGE", "100", "Regalo"), item(10, 1))))
                .isInstanceOf(ValidationErrorException.class)
                .hasMessageContaining("mayor a 0");
    }

    @Test
    void price_rejectsFixedDiscountAboveSubtotal() {
        asRoles(RoleCodes.ADMIN);
        assertThatThrownBy(() -> service.price(quote(discount("FIXED_AMOUNT", "20", "x"), item(10, 1))))
                .isInstanceOf(ValidationErrorException.class);
    }

    @Test
    void price_rejectsSoldOutProduct() {
        capuchino.setAvailable(false);
        assertThatThrownBy(() -> service.price(quote(null, item(10, 1))))
                .isInstanceOf(ValidationErrorException.class)
                .hasMessageContaining("agotado");
    }

    @Test
    void price_rejectsInactiveCategory() {
        bebidas.setActive(false);
        assertThatThrownBy(() -> service.price(quote(null, item(10, 1))))
                .isInstanceOf(ValidationErrorException.class);
    }

    @Test
    void price_rejectsAddonNotAssignedToProduct() {
        assertThatThrownBy(() -> service.price(quote(null, item(10, 1, 99))))
                .isInstanceOf(ValidationErrorException.class);
    }

    @Test
    void price_rejectsInactiveAddon() {
        almendra.setActive(false);
        assertThatThrownBy(() -> service.price(quote(null, item(10, 1, 5))))
                .isInstanceOf(ValidationErrorException.class);
    }

    @Test
    void price_rejectsRepeatedAddon() {
        assertThatThrownBy(() -> service.price(quote(null, item(10, 1, 5, 5))))
                .isInstanceOf(ValidationErrorException.class)
                .hasMessageContaining("repetidos");
    }

    @Test
    void price_usesCurrentCatalogPrice_notAnythingElse() {
        capuchino.setPrice(new BigDecimal("16.005"));
        PricedOrder priced = service.price(quote(null, item(10, 1)));
        assertThat(priced.lines().getFirst().unitPrice()).isEqualByComparingTo("16.01");
    }
}
