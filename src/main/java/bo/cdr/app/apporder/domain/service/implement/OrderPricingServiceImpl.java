package bo.cdr.app.apporder.domain.service.implement;

import bo.cdr.app.appcatalog.domain.entity.Addon;
import bo.cdr.app.appcatalog.domain.entity.Category;
import bo.cdr.app.appcatalog.domain.entity.Product;
import bo.cdr.app.appcatalog.domain.service.interfaces.ParameterService;
import bo.cdr.app.appcatalog.domain.service.interfaces.ProductService;
import bo.cdr.app.apporder.domain.model.PricedOrder;
import bo.cdr.app.apporder.domain.service.interfaces.OrderPricingService;
import bo.cdr.app.apporder.presentation.request.dto.DiscountDto;
import bo.cdr.app.apporder.presentation.request.dto.OrderItemDto;
import bo.cdr.app.apporder.presentation.request.dto.OrderQuoteDto;
import bo.cdr.app.appsecurity.auth.jwt.JwtService;
import bo.cdr.app.common.MoneyUtil;
import bo.cdr.app.common.enums.DiscountTypeEnum;
import bo.cdr.app.exception.NotAllowedException;
import bo.cdr.app.exception.response.ValidationErrorException;
import bo.cdr.app.util.constants.ParameterCodes;
import bo.cdr.app.util.constants.RoleCodes;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@RequiredArgsConstructor
public class OrderPricingServiceImpl implements OrderPricingService {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final ProductService productService;
    private final ParameterService parameterService;
    private final JwtService jwtService;

    @Override
    @Transactional(readOnly = true)
    public PricedOrder price(OrderQuoteDto dto) {
        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new ValidationErrorException("El pedido no tiene productos.");
        }
        List<PricedOrder.Line> lines = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        for (OrderItemDto item : dto.getItems()) {
            PricedOrder.Line line = priceLine(item);
            lines.add(line);
            subtotal = subtotal.add(line.lineSubtotal());
        }
        subtotal = MoneyUtil.round(subtotal);

        DiscountDto discount = dto.getDiscount();
        if (discount == null) {
            return finish(lines, subtotal, null, null, BigDecimal.ZERO.setScale(MoneyUtil.SCALE), null, null);
        }

        DiscountTypeEnum type = DiscountTypeEnum.fromString(discount.getType());
        BigDecimal value = MoneyUtil.round(discount.getValue());
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationErrorException("El valor del descuento debe ser mayor a 0.");
        }
        BigDecimal discountAmount = switch (type) {
            case PERCENTAGE -> {
                if (value.compareTo(ONE_HUNDRED) > 0) {
                    throw new ValidationErrorException("El descuento porcentual no puede superar el 100 %.");
                }
                yield MoneyUtil.percentOf(subtotal, value);
            }
            case FIXED_AMOUNT -> {
                if (value.compareTo(subtotal) > 0) {
                    throw new ValidationErrorException(String.format(
                            "El descuento (Bs %s) no puede superar el subtotal (Bs %s).",
                            value.toPlainString(), subtotal.toPlainString()));
                }
                yield value;
            }
        };
        BigDecimal effectivePercent = subtotal.signum() == 0 ? BigDecimal.ZERO
                : discountAmount.multiply(ONE_HUNDRED).divide(subtotal, 4, RoundingMode.HALF_UP);

        validateDiscountPermission(effectivePercent);
        String reason = discount.getReason() == null || discount.getReason().isBlank() ? null : discount.getReason().trim();
        BigDecimal reasonThreshold = parameterService.getDecimalValue(ParameterCodes.DISCOUNT_REASON_REQUIRED_ABOVE_PERCENT);
        if (reason == null && effectivePercent.compareTo(reasonThreshold) > 0) {
            throw new ValidationErrorException("Debe indicar el motivo del descuento.");
        }
        return finish(lines, subtotal, type, value, discountAmount, effectivePercent, reason);
    }

    private PricedOrder finish(List<PricedOrder.Line> lines, BigDecimal subtotal, DiscountTypeEnum type,
                               BigDecimal value, BigDecimal discountAmount, BigDecimal percent, String reason) {
        BigDecimal total = MoneyUtil.round(subtotal.subtract(discountAmount));
        // Decisión aprobada (Etapa 3): no se registran pedidos con total 0
        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationErrorException("El total del pedido debe ser mayor a 0.");
        }
        BigDecimal roundedPercent = percent == null ? null : percent.setScale(2, RoundingMode.HALF_UP);
        return new PricedOrder(List.copyOf(lines), subtotal, type, value, MoneyUtil.round(discountAmount),
                roundedPercent, reason, total);
    }

    private PricedOrder.Line priceLine(OrderItemDto item) {
        Product product = productService.getById(item.getProductId());
        Category category = product.getCategory();
        if (!Boolean.TRUE.equals(product.getActive()) || !Boolean.TRUE.equals(category.getActive())) {
            throw new ValidationErrorException("El producto '" + product.getName() + "' ya no está a la venta.");
        }
        if (!Boolean.TRUE.equals(product.getAvailable())) {
            throw new ValidationErrorException("El producto '" + product.getName() + "' está agotado.");
        }
        int quantity = item.getQuantity() == null ? 0 : item.getQuantity();
        if (quantity < 1) {
            throw new ValidationErrorException("La cantidad debe ser al menos 1.");
        }

        List<Integer> addonIds = item.getAddonIds() == null ? List.of() : item.getAddonIds();
        if (new HashSet<>(addonIds).size() != addonIds.size()) {
            throw new ValidationErrorException("Hay adicionales repetidos en '" + product.getName() + "'.");
        }
        Map<Integer, Addon> allowed = new HashMap<>();
        product.getAddons().forEach(a -> allowed.put(a.getId(), a));
        List<Addon> addons = new ArrayList<>();
        BigDecimal addonsUnitTotal = BigDecimal.ZERO;
        for (Integer addonId : addonIds) {
            Addon addon = allowed.get(addonId);
            if (addon == null || !Boolean.TRUE.equals(addon.getActive())) {
                throw new ValidationErrorException("Un adicional elegido no está disponible para '" + product.getName() + "'.");
            }
            addons.add(addon);
            addonsUnitTotal = addonsUnitTotal.add(addon.getPrice());
        }
        addons.sort(Comparator.comparing(Addon::getId));

        BigDecimal unitPrice = MoneyUtil.round(product.getPrice());
        addonsUnitTotal = MoneyUtil.round(addonsUnitTotal);
        BigDecimal lineSubtotal = MoneyUtil.round(unitPrice.add(addonsUnitTotal).multiply(BigDecimal.valueOf(quantity)));
        String notes = item.getNotes() == null || item.getNotes().isBlank() ? null : item.getNotes().trim();
        return new PricedOrder.Line(product, category.getName(), category.getId(), quantity, unitPrice,
                List.copyOf(addons), addonsUnitTotal, lineSubtotal, notes);
    }

    /** COCINA no descuenta; CAJERO hasta CASHIER_MAX_DISCOUNT_PERCENT; ADMIN sin límite. */
    private void validateDiscountPermission(BigDecimal effectivePercent) {
        List<String> roles = jwtService.getCurrentAuthorities();
        if (roles.contains(RoleCodes.ADMIN)) {
            return;
        }
        if (!roles.contains(RoleCodes.CAJERO)) {
            throw new NotAllowedException("Su rol no permite aplicar descuentos.");
        }
        BigDecimal max = parameterService.getDecimalValue(ParameterCodes.CASHIER_MAX_DISCOUNT_PERCENT);
        if (effectivePercent.compareTo(max) > 0) {
            throw new NotAllowedException(String.format(
                    "El descuento (%s %%) supera el máximo permitido al cajero (%s %%).",
                    effectivePercent.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString(),
                    max.stripTrailingZeros().toPlainString()));
        }
    }
}
