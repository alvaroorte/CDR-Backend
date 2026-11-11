package bo.cdr.app.appreport.domain.service.implement;

import bo.cdr.app.appreport.data.ReportRepository;
import bo.cdr.app.appreport.domain.service.interfaces.ReportExcelService;
import bo.cdr.app.appreport.presentation.request.filter.ReportFilter;
import bo.cdr.app.appreport.presentation.response.pojo.AttachmentPojo;
import bo.cdr.app.appreport.presentation.response.pojo.OperationsReportPojo;
import bo.cdr.app.appreport.presentation.response.pojo.PaymentsReportPojo;
import bo.cdr.app.appreport.presentation.response.pojo.ProductsReportPojo;
import bo.cdr.app.appreport.presentation.response.pojo.SalesReportPojo;
import bo.cdr.app.exception.InvalidEnumValueException;
import bo.cdr.app.exception.MissingParameterException;
import bo.cdr.app.exception.NotAllowedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    private static final LocalDate FROM = LocalDate.of(2026, 11, 1);
    private static final LocalDate TO = LocalDate.of(2026, 11, 11);

    @Mock ReportRepository reportRepository;
    @Mock ReportExcelService reportExcelService;
    ReportServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ReportServiceImpl(reportRepository, reportExcelService);
    }

    private static ReportFilter range() {
        return ReportFilter.builder().dateFrom(FROM).dateTo(TO).build();
    }

    private static <T> T row(Class<T> type) {
        return mock(type, withSettings().strictness(Strictness.LENIENT));
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    private void givenSummary(int userId, String serviceType, long orders, String gross, String discount, String net) {
        ReportRepository.SalesSummaryRow s = summary(orders, gross, discount, net);
        when(reportRepository.salesSummary(FROM, TO, userId, serviceType)).thenReturn(s);
    }

    private ReportRepository.SalesSummaryRow summary(long orders, String gross, String discount, String net) {
        ReportRepository.SalesSummaryRow s = row(ReportRepository.SalesSummaryRow.class);
        when(s.getOrderCount()).thenReturn(orders);
        when(s.getGross()).thenReturn(bd(gross));
        when(s.getDiscount()).thenReturn(bd(discount));
        when(s.getNet()).thenReturn(bd(net));
        when(s.getPaid()).thenReturn(bd(net));
        when(s.getRefunded()).thenReturn(bd("15.5"));
        when(s.getCancelledCount()).thenReturn(1L);
        when(s.getCancelledAmount()).thenReturn(bd("15.5"));
        return s;
    }

    // ------------------------------------------------------------------ validación de filtros

    @Test
    void getSales_throws_whenDateFromMissing() {
        assertThatThrownBy(() -> service.getSales(ReportFilter.builder().dateTo(TO).build()))
                .isInstanceOf(MissingParameterException.class);
        verifyNoInteractions(reportRepository);
    }

    @Test
    void getSales_throws_whenDateToMissing() {
        assertThatThrownBy(() -> service.getSales(ReportFilter.builder().dateFrom(FROM).build()))
                .isInstanceOf(MissingParameterException.class);
    }

    @Test
    void getSales_throws_whenRangeInverted() {
        assertThatThrownBy(() -> service.getSales(ReportFilter.builder().dateFrom(TO).dateTo(FROM).build()))
                .isInstanceOf(NotAllowedException.class);
    }

    @Test
    void getSales_throws_whenServiceTypeInvalid() {
        ReportFilter filter = range();
        filter.setServiceType("DELIVERY");
        assertThatThrownBy(() -> service.getSales(filter)).isInstanceOf(InvalidEnumValueException.class);
    }

    @Test
    void getSales_throws_whenGroupByInvalid() {
        ReportFilter filter = range();
        filter.setGroupBy("YEAR");
        assertThatThrownBy(() -> service.getSales(filter)).isInstanceOf(InvalidEnumValueException.class);
    }

    @Test
    void optionalFilters_useSentinels_andNormalizeServiceType() {
        givenSummary(0, "", 0, "0", "0", "0");
        service.getSales(range());
        verify(reportRepository).salesSummary(FROM, TO, 0, "");
        verify(reportRepository).salesByPeriod("day", FROM, TO, 0, "");

        ReportFilter filter = range();
        filter.setUserId(4);
        filter.setServiceType("takeaway");
        filter.setGroupBy("month");
        givenSummary(4, "TAKEAWAY", 0, "0", "0", "0");
        service.getSales(filter);
        verify(reportRepository).salesByPeriod("month", FROM, TO, 4, "TAKEAWAY");
    }

    // ------------------------------------------------------------------ ventas

    @Test
    void getSales_buildsSummary_withAverageTicketRoundedHalfUp() {
        givenSummary(0, "", 3, "52.00", "2.00", "50.00");
        ReportRepository.SalesPeriodRow period = row(ReportRepository.SalesPeriodRow.class);
        when(period.getPeriodStart()).thenReturn(FROM);
        when(period.getOrderCount()).thenReturn(3L);
        when(period.getGross()).thenReturn(bd("52.00"));
        when(period.getDiscount()).thenReturn(bd("2.00"));
        when(period.getNet()).thenReturn(bd("50.00"));
        when(reportRepository.salesByPeriod("day", FROM, TO, 0, "")).thenReturn(List.of(period));
        ReportRepository.ServiceTypeRow dineIn = row(ReportRepository.ServiceTypeRow.class);
        when(dineIn.getServiceType()).thenReturn("DINE_IN");
        when(dineIn.getOrderCount()).thenReturn(2L);
        when(dineIn.getNet()).thenReturn(bd("30.00"));
        ReportRepository.ServiceTypeRow takeaway = row(ReportRepository.ServiceTypeRow.class);
        when(takeaway.getServiceType()).thenReturn("TAKEAWAY");
        when(takeaway.getOrderCount()).thenReturn(1L);
        when(takeaway.getNet()).thenReturn(bd("20.00"));
        when(reportRepository.salesByServiceType(FROM, TO, 0, "")).thenReturn(List.of(dineIn, takeaway));

        SalesReportPojo report = service.getSales(range());

        assertThat(report.groupBy()).isEqualTo("DAY");
        assertThat(report.groupByLabel()).isEqualTo("Día");
        assertThat(report.summary().net()).isEqualByComparingTo("50.00");
        // 50 / 3 = 16.6666… → 16.67
        assertThat(report.summary().averageTicket()).isEqualTo(bd("16.67"));
        assertThat(report.summary().refunded()).isEqualTo(bd("15.50"));
        assertThat(report.periods()).hasSize(1);
        assertThat(report.serviceTypes()).extracting(SalesReportPojo.ServiceType::serviceTypeLabel)
                .containsExactly("En el local", "Para llevar");
        assertThat(report.serviceTypes()).extracting(SalesReportPojo.ServiceType::sharePercent)
                .containsExactly(bd("60.00"), bd("40.00"));
    }

    @Test
    void getSales_averageTicketIsZero_whenNoOrders() {
        givenSummary(0, "", 0, "0", "0", "0");
        SalesReportPojo report = service.getSales(range());
        assertThat(report.summary().averageTicket()).isEqualTo(bd("0.00"));
        assertThat(report.serviceTypes()).isEmpty();
    }

    // ------------------------------------------------------------------ productos

    @Test
    void getProducts_mapsRows_andComputesShare_passingCategoryFilter() {
        ReportRepository.ProductRow capuchino = row(ReportRepository.ProductRow.class);
        when(capuchino.getProductId()).thenReturn(1);
        when(capuchino.getProductName()).thenReturn("Capuchino");
        when(capuchino.getCategoryName()).thenReturn("Bebidas");
        when(capuchino.getQuantity()).thenReturn(3L);
        when(capuchino.getOrderCount()).thenReturn(2L);
        when(capuchino.getGross()).thenReturn(bd("20"));
        ReportRepository.ProductRow latte = row(ReportRepository.ProductRow.class);
        when(latte.getProductName()).thenReturn("Latte");
        when(latte.getQuantity()).thenReturn(1L);
        when(latte.getOrderCount()).thenReturn(1L);
        when(latte.getGross()).thenReturn(bd("10"));
        ReportFilter filter = range();
        filter.setCategoryId(2);
        when(reportRepository.productSales(FROM, TO, 0, "", 2)).thenReturn(List.of(capuchino, latte));
        when(reportRepository.categorySales(FROM, TO, 0, "", 2)).thenReturn(List.of());

        ProductsReportPojo report = service.getProducts(filter);

        assertThat(report.products()).extracting(ProductsReportPojo.Product::sharePercent)
                .containsExactly(bd("66.67"), bd("33.33"));
        assertThat(report.products().getFirst().gross()).isEqualTo(bd("20.00"));
        assertThat(report.categories()).isEmpty();
    }

    // ------------------------------------------------------------------ pagos y cajas

    @Test
    void getPayments_mapsMethodsAndSessions_keepingNullCloseTotalsForOpenSessions() {
        ReportRepository.PaymentMethodRow cash = row(ReportRepository.PaymentMethodRow.class);
        when(cash.getPaymentMethodId()).thenReturn(1);
        when(cash.getPaymentMethodName()).thenReturn("Efectivo");
        when(cash.getPaidCount()).thenReturn(2L);
        when(cash.getPaid()).thenReturn(bd("31.00"));
        when(cash.getRefundedCount()).thenReturn(1L);
        when(cash.getRefunded()).thenReturn(bd("15.50"));
        when(reportRepository.paymentMethods(FROM, TO, 0, "", 0)).thenReturn(List.of(cash));
        ReportRepository.CashSessionRow open = row(ReportRepository.CashSessionRow.class);
        when(open.getId()).thenReturn(7);
        when(open.getStatus()).thenReturn("OPEN");
        when(open.getOpenedAt()).thenReturn(LocalDateTime.of(2026, 11, 11, 8, 0));
        when(open.getOpeningAmount()).thenReturn(bd("100"));
        when(open.getSalesTotal()).thenReturn(bd("31"));
        when(reportRepository.cashSessions(FROM, TO, 0)).thenReturn(List.of(open));

        PaymentsReportPojo report = service.getPayments(range());

        assertThat(report.paymentMethods().getFirst().sharePercent()).isEqualTo(bd("100.00"));
        assertThat(report.paymentMethods().getFirst().refunded()).isEqualTo(bd("15.50"));
        PaymentsReportPojo.CashSession session = report.cashSessions().getFirst();
        assertThat(session.statusLabel()).isEqualTo("Abierta");
        assertThat(session.openingAmount()).isEqualTo(bd("100.00"));
        assertThat(session.expectedTotal()).isNull();
        assertThat(session.differenceTotal()).isNull();
    }

    // ------------------------------------------------------------------ operaciones

    @Test
    void getOperations_listsEveryStatusInFlowOrder_andLabelsCodesAndDiscounts() {
        ReportRepository.StatusRow delivered = row(ReportRepository.StatusRow.class);
        when(delivered.getStatus()).thenReturn("DELIVERED");
        when(delivered.getOrderCount()).thenReturn(4L);
        when(delivered.getTotal()).thenReturn(bd("62"));
        when(reportRepository.ordersByStatus(FROM, TO, 0, "")).thenReturn(List.of(delivered));
        when(reportRepository.salesByUser(FROM, TO, 0, "")).thenReturn(List.of());
        ReportRepository.DiscountRow discount = row(ReportRepository.DiscountRow.class);
        when(discount.getOrderId()).thenReturn(10);
        when(discount.getCode()).thenReturn(5);
        when(discount.getDiscountType()).thenReturn("PERCENTAGE");
        when(discount.getDiscountValue()).thenReturn(bd("10.00"));
        when(discount.getSubtotal()).thenReturn(bd("20"));
        when(discount.getDiscountAmount()).thenReturn(bd("2"));
        when(discount.getTotal()).thenReturn(bd("18"));
        when(discount.getDiscountReason()).thenReturn("Cliente frecuente");
        when(reportRepository.discounts(FROM, TO, 0, "")).thenReturn(List.of(discount));
        ReportRepository.CancellationRow cancelled = row(ReportRepository.CancellationRow.class);
        when(cancelled.getCode()).thenReturn(12);
        when(cancelled.getTotal()).thenReturn(bd("15.5"));
        when(cancelled.getCancelReason()).thenReturn("Cliente se retiró");
        when(reportRepository.cancellations(FROM, TO, 0, "")).thenReturn(List.of(cancelled));

        OperationsReportPojo report = service.getOperations(range());

        assertThat(report.statuses()).extracting(OperationsReportPojo.StatusCount::statusLabel)
                .containsExactly("PENDIENTE", "EN_PREPARACION", "LISTO", "ENTREGADO", "CANCELADO");
        assertThat(report.statuses()).extracting(OperationsReportPojo.StatusCount::orderCount)
                .containsExactly(0L, 0L, 0L, 4L, 0L);
        assertThat(report.statuses().getFirst().total()).isEqualTo(bd("0.00"));
        OperationsReportPojo.DiscountOrder d = report.discounts().getFirst();
        assertThat(d.codeLabel()).isEqualTo("#005");
        assertThat(d.discountTypeLabel()).isEqualTo("Porcentaje");
        assertThat(d.discountAmount()).isEqualTo(bd("2.00"));
        assertThat(report.cancellations().getFirst().codeLabel()).isEqualTo("#012");
        assertThat(report.cancellations().getFirst().total()).isEqualTo(bd("15.50"));
    }

    // ------------------------------------------------------------------ exportación

    @Test
    void export_buildsBase64Excel_forRequestedReport() {
        givenSummary(0, "", 0, "0", "0", "0");
        byte[] content = {1, 2, 3};
        when(reportExcelService.sales(any(SalesReportPojo.class))).thenReturn(content);

        AttachmentPojo attachment = service.export("sales", range());

        assertThat(attachment.typeFile()).isEqualTo("EXCEL");
        assertThat(attachment.name()).isEqualTo("Reporte Ventas 2026-11-01 a 2026-11-11.xlsx");
        assertThat(Base64.getDecoder().decode(attachment.file())).isEqualTo(content);
    }

    @Test
    void export_throws_whenReportUnknown() {
        assertThatThrownBy(() -> service.export("FOO", range())).isInstanceOf(InvalidEnumValueException.class);
        verifyNoInteractions(reportExcelService);
    }

    @Test
    void share_isZero_whenTotalIsZero() {
        assertThat(ReportServiceImpl.share(bd("5"), BigDecimal.ZERO)).isEqualTo(bd("0.00"));
    }
}
