package bo.cdr.app.appreport.domain.service.implement;

import bo.cdr.app.appreport.data.ReportRepository;
import bo.cdr.app.appreport.domain.service.interfaces.ReportExcelService;
import bo.cdr.app.appreport.domain.service.interfaces.ReportService;
import bo.cdr.app.appreport.presentation.request.filter.ReportFilter;
import bo.cdr.app.appreport.presentation.response.pojo.AttachmentPojo;
import bo.cdr.app.appreport.presentation.response.pojo.OperationsReportPojo;
import bo.cdr.app.appreport.presentation.response.pojo.PaymentsReportPojo;
import bo.cdr.app.appreport.presentation.response.pojo.ProductsReportPojo;
import bo.cdr.app.appreport.presentation.response.pojo.SalesReportPojo;
import bo.cdr.app.apporder.domain.mapper.OrderMapper;
import bo.cdr.app.common.MoneyUtil;
import bo.cdr.app.common.enums.CashSessionStatusEnum;
import bo.cdr.app.common.enums.DiscountTypeEnum;
import bo.cdr.app.common.enums.OrderStatusEnum;
import bo.cdr.app.common.enums.ReportGroupByEnum;
import bo.cdr.app.common.enums.ReportTypeEnum;
import bo.cdr.app.common.enums.ServiceTypeEnum;
import bo.cdr.app.exception.MissingParameterException;
import bo.cdr.app.exception.NotAllowedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Reportes (skill cdr-order-management §12): todo sale de las fotografías del pedido; la lógica de
 * aquí es solo validar filtros, traducir etiquetas y calcular participaciones / ticket promedio.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    /** Centinelas de "sin filtro" (ver ReportRepository). */
    private static final int NO_ID = 0;
    private static final String NO_TEXT = "";

    private final ReportRepository reportRepository;
    private final ReportExcelService reportExcelService;

    /** Filtros ya validados y normalizados a centinelas. */
    private record Params(LocalDate from, LocalDate to, int userId, String serviceType,
                          int categoryId, int paymentMethodId) {
    }

    @Override
    @Transactional(readOnly = true)
    public SalesReportPojo getSales(ReportFilter filter) {
        Params p = validate(filter);
        ReportGroupByEnum groupBy = ReportGroupByEnum.fromString(filter.getGroupBy());

        ReportRepository.SalesSummaryRow s = reportRepository.salesSummary(p.from(), p.to(), p.userId(), p.serviceType());
        long orderCount = count(s.getOrderCount());
        BigDecimal net = money(s.getNet());
        SalesReportPojo.Summary summary = new SalesReportPojo.Summary(orderCount, money(s.getGross()),
                money(s.getDiscount()), net, money(s.getPaid()), money(s.getRefunded()),
                orderCount == 0 ? MoneyUtil.round(BigDecimal.ZERO)
                        : MoneyUtil.round(net.divide(BigDecimal.valueOf(orderCount), MoneyUtil.SCALE + 4, RoundingMode.HALF_UP)),
                count(s.getCancelledCount()), money(s.getCancelledAmount()));

        List<SalesReportPojo.Period> periods = reportRepository
                .salesByPeriod(groupBy.getUnit(), p.from(), p.to(), p.userId(), p.serviceType()).stream()
                .map(r -> new SalesReportPojo.Period(r.getPeriodStart(), count(r.getOrderCount()), money(r.getGross()),
                        money(r.getDiscount()), money(r.getNet())))
                .toList();

        List<ReportRepository.ServiceTypeRow> typeRows = reportRepository
                .salesByServiceType(p.from(), p.to(), p.userId(), p.serviceType());
        BigDecimal typesTotal = sum(typeRows, ReportRepository.ServiceTypeRow::getNet);
        List<SalesReportPojo.ServiceType> serviceTypes = typeRows.stream()
                .map(r -> new SalesReportPojo.ServiceType(r.getServiceType(),
                        ServiceTypeEnum.fromString(r.getServiceType()).getLabel(), count(r.getOrderCount()),
                        money(r.getNet()), share(r.getNet(), typesTotal)))
                .toList();

        return new SalesReportPojo(p.from(), p.to(), groupBy.name(), groupBy.getLabel(), summary, periods, serviceTypes);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductsReportPojo getProducts(ReportFilter filter) {
        Params p = validate(filter);

        List<ReportRepository.ProductRow> productRows = reportRepository
                .productSales(p.from(), p.to(), p.userId(), p.serviceType(), p.categoryId());
        BigDecimal productsTotal = sum(productRows, ReportRepository.ProductRow::getGross);
        List<ProductsReportPojo.Product> products = productRows.stream()
                .map(r -> new ProductsReportPojo.Product(r.getProductId(), r.getProductName(), r.getCategoryName(),
                        count(r.getQuantity()), count(r.getOrderCount()), money(r.getGross()),
                        share(r.getGross(), productsTotal)))
                .toList();

        List<ReportRepository.CategoryRow> categoryRows = reportRepository
                .categorySales(p.from(), p.to(), p.userId(), p.serviceType(), p.categoryId());
        BigDecimal categoriesTotal = sum(categoryRows, ReportRepository.CategoryRow::getGross);
        List<ProductsReportPojo.Category> categories = categoryRows.stream()
                .map(r -> new ProductsReportPojo.Category(r.getCategoryId(), r.getCategoryName(), count(r.getQuantity()),
                        money(r.getGross()), share(r.getGross(), categoriesTotal)))
                .toList();

        return new ProductsReportPojo(p.from(), p.to(), products, categories);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentsReportPojo getPayments(ReportFilter filter) {
        Params p = validate(filter);

        List<ReportRepository.PaymentMethodRow> methodRows = reportRepository
                .paymentMethods(p.from(), p.to(), p.userId(), p.serviceType(), p.paymentMethodId());
        BigDecimal paidTotal = sum(methodRows, ReportRepository.PaymentMethodRow::getPaid);
        List<PaymentsReportPojo.PaymentMethod> methods = methodRows.stream()
                .map(r -> new PaymentsReportPojo.PaymentMethod(r.getPaymentMethodId(), r.getPaymentMethodName(),
                        count(r.getPaidCount()), money(r.getPaid()), count(r.getRefundedCount()), money(r.getRefunded()),
                        share(r.getPaid(), paidTotal)))
                .toList();

        List<PaymentsReportPojo.CashSession> sessions = reportRepository.cashSessions(p.from(), p.to(), p.userId()).stream()
                .map(r -> new PaymentsReportPojo.CashSession(r.getId(), r.getUsername(), r.getUserFullName(), r.getStatus(),
                        CashSessionStatusEnum.fromString(r.getStatus()).getLabel(), r.getOpenedAt(), r.getClosedAt(),
                        r.getClosedBy(), money(r.getOpeningAmount()), money(r.getSalesTotal()),
                        MoneyUtil.round(r.getExpectedTotal()), MoneyUtil.round(r.getDeclaredTotal()),
                        MoneyUtil.round(r.getDifferenceTotal())))
                .toList();

        return new PaymentsReportPojo(p.from(), p.to(), methods, sessions);
    }

    @Override
    @Transactional(readOnly = true)
    public OperationsReportPojo getOperations(ReportFilter filter) {
        Params p = validate(filter);

        List<OperationsReportPojo.UserSales> users = reportRepository
                .salesByUser(p.from(), p.to(), p.userId(), p.serviceType()).stream()
                .map(r -> new OperationsReportPojo.UserSales(r.getUserId(), r.getUsername(), r.getUserFullName(),
                        count(r.getOrderCount()), money(r.getGross()), money(r.getDiscount()), money(r.getNet()),
                        count(r.getCancelledCount())))
                .toList();

        // Todos los estados en el orden del flujo, con 0 los que no tienen pedidos
        Map<String, ReportRepository.StatusRow> byStatus = reportRepository
                .ordersByStatus(p.from(), p.to(), p.userId(), p.serviceType()).stream()
                .collect(Collectors.toMap(ReportRepository.StatusRow::getStatus, Function.identity()));
        List<OperationsReportPojo.StatusCount> statuses = Arrays.stream(OrderStatusEnum.values())
                .map(status -> {
                    ReportRepository.StatusRow row = byStatus.get(status.name());
                    return new OperationsReportPojo.StatusCount(status.name(), status.getLabel(),
                            row == null ? 0 : count(row.getOrderCount()), money(row == null ? null : row.getTotal()));
                })
                .toList();

        List<OperationsReportPojo.DiscountOrder> discounts = reportRepository
                .discounts(p.from(), p.to(), p.userId(), p.serviceType()).stream()
                .map(r -> new OperationsReportPojo.DiscountOrder(r.getOrderId(), OrderMapper.codeLabel(r.getCode()),
                        r.getBusinessDate(), r.getCreatedAt(), r.getCashier(), r.getDiscountType(),
                        r.getDiscountType() == null ? null : DiscountTypeEnum.fromString(r.getDiscountType()).getLabel(),
                        r.getDiscountValue(), money(r.getSubtotal()), money(r.getDiscountAmount()), money(r.getTotal()),
                        r.getDiscountReason(), r.getAppliedBy()))
                .toList();

        List<OperationsReportPojo.CancelledOrder> cancellations = reportRepository
                .cancellations(p.from(), p.to(), p.userId(), p.serviceType()).stream()
                .map(r -> new OperationsReportPojo.CancelledOrder(r.getOrderId(), OrderMapper.codeLabel(r.getCode()),
                        r.getBusinessDate(), r.getCreatedAt(), r.getCashier(), money(r.getTotal()), r.getCancelReason(),
                        r.getCancelledBy(), r.getCancelledAt()))
                .toList();

        return new OperationsReportPojo(p.from(), p.to(), users, statuses, discounts, cancellations);
    }

    @Override
    @Transactional(readOnly = true)
    public AttachmentPojo export(String report, ReportFilter filter) {
        ReportTypeEnum type = ReportTypeEnum.fromString(report);
        byte[] content = switch (type) {
            case SALES -> reportExcelService.sales(getSales(filter));
            case PRODUCTS -> reportExcelService.products(getProducts(filter));
            case PAYMENTS -> reportExcelService.payments(getPayments(filter));
            case OPERATIONS -> reportExcelService.operations(getOperations(filter));
        };
        String name = String.format("Reporte %s %s a %s.xlsx", type.getLabel(), filter.getDateFrom(), filter.getDateTo());
        log.info("Reporte {} exportado ({} a {})", type, filter.getDateFrom(), filter.getDateTo());
        return new AttachmentPojo(name, Base64.getEncoder().encodeToString(content), AttachmentPojo.TYPE_EXCEL);
    }

    // ------------------------------------------------------------------ helpers

    private Params validate(ReportFilter filter) {
        if (filter == null || filter.getDateFrom() == null) {
            throw new MissingParameterException("Fecha desde");
        }
        if (filter.getDateTo() == null) {
            throw new MissingParameterException("Fecha hasta");
        }
        if (filter.getDateFrom().isAfter(filter.getDateTo())) {
            throw new NotAllowedException("La fecha desde no puede ser posterior a la fecha hasta.");
        }
        String serviceType = filter.getServiceType() == null || filter.getServiceType().isBlank()
                ? NO_TEXT : ServiceTypeEnum.fromString(filter.getServiceType()).name();
        return new Params(filter.getDateFrom(), filter.getDateTo(), idOrNone(filter.getUserId()), serviceType,
                idOrNone(filter.getCategoryId()), idOrNone(filter.getPaymentMethodId()));
    }

    private static int idOrNone(Integer id) {
        return id == null || id <= 0 ? NO_ID : id;
    }

    private static long count(Long value) {
        return value == null ? 0 : value;
    }

    private static BigDecimal money(BigDecimal value) {
        return MoneyUtil.round(value == null ? BigDecimal.ZERO : value);
    }

    private static <T> BigDecimal sum(List<T> rows, Function<T, BigDecimal> amount) {
        return rows.stream().map(amount).map(ReportServiceImpl::money).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Participación en %, escala 2 HALF_UP; 0 si el total es 0. */
    static BigDecimal share(BigDecimal value, BigDecimal total) {
        if (total == null || total.signum() == 0) {
            return MoneyUtil.round(BigDecimal.ZERO);
        }
        return MoneyUtil.round(money(value).multiply(ONE_HUNDRED).divide(total, MoneyUtil.SCALE + 4, RoundingMode.HALF_UP));
    }
}
