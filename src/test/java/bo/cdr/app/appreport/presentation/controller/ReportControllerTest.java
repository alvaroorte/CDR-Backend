package bo.cdr.app.appreport.presentation.controller;

import bo.cdr.app.appreport.domain.service.interfaces.ReportService;
import bo.cdr.app.appreport.presentation.request.filter.ReportFilter;
import bo.cdr.app.appreport.presentation.response.pojo.AttachmentPojo;
import bo.cdr.app.appreport.presentation.response.pojo.OperationsReportPojo;
import bo.cdr.app.appreport.presentation.response.pojo.PaymentsReportPojo;
import bo.cdr.app.appreport.presentation.response.pojo.ProductsReportPojo;
import bo.cdr.app.appreport.presentation.response.pojo.SalesReportPojo;
import bo.cdr.app.exception.InvalidEnumValueException;
import bo.cdr.app.exception.MissingParameterException;
import bo.cdr.app.util.MockMvcTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ReportControllerTest {

    private static final LocalDate FROM = LocalDate.of(2026, 11, 1);
    private static final LocalDate TO = LocalDate.of(2026, 11, 11);

    @Mock ReportService reportService;
    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcTestSupport.buildMockMvc(new ReportController(reportService));
    }

    @Test
    void sales_returns200_andBindsFilters() throws Exception {
        SalesReportPojo.Summary summary = new SalesReportPojo.Summary(2, new BigDecimal("31.00"), new BigDecimal("1.00"),
                new BigDecimal("30.00"), new BigDecimal("30.00"), BigDecimal.ZERO, new BigDecimal("15.00"), 0, BigDecimal.ZERO);
        when(reportService.getSales(any(ReportFilter.class)))
                .thenReturn(new SalesReportPojo(FROM, TO, "DAY", "Día", summary, List.of(), List.of()));

        mockMvc.perform(get("/report/sales").param("dateFrom", "2026-11-01").param("dateTo", "2026-11-11")
                        .param("groupBy", "WEEK").param("userId", "4").param("serviceType", "TAKEAWAY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.net").value(30.00))
                .andExpect(jsonPath("$.summary.averageTicket").value(15.00));

        ArgumentCaptor<ReportFilter> captor = ArgumentCaptor.forClass(ReportFilter.class);
        verify(reportService).getSales(captor.capture());
        assertThat(captor.getValue().getDateFrom()).isEqualTo(FROM);
        assertThat(captor.getValue().getDateTo()).isEqualTo(TO);
        assertThat(captor.getValue().getGroupBy()).isEqualTo("WEEK");
        assertThat(captor.getValue().getUserId()).isEqualTo(4);
        assertThat(captor.getValue().getServiceType()).isEqualTo("TAKEAWAY");
    }

    @Test
    void sales_returns400_whenDatesMissing() throws Exception {
        when(reportService.getSales(any(ReportFilter.class))).thenThrow(new MissingParameterException("Fecha desde"));
        mockMvc.perform(get("/report/sales")).andExpect(status().isBadRequest());
    }

    @Test
    void sales_returns400_whenDateMalformed() throws Exception {
        mockMvc.perform(get("/report/sales").param("dateFrom", "11/01/2026").param("dateTo", "2026-11-11"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void products_returns200() throws Exception {
        when(reportService.getProducts(any(ReportFilter.class))).thenReturn(new ProductsReportPojo(FROM, TO,
                List.of(new ProductsReportPojo.Product(1, "Capuchino", "Bebidas", 3, 2, new BigDecimal("46.50"),
                        new BigDecimal("100.00"))), List.of()));
        mockMvc.perform(get("/report/products").param("dateFrom", "2026-11-01").param("dateTo", "2026-11-11")
                        .param("categoryId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products[0].productName").value("Capuchino"))
                .andExpect(jsonPath("$.products[0].quantity").value(3));
    }

    @Test
    void payments_returns200() throws Exception {
        when(reportService.getPayments(any(ReportFilter.class)))
                .thenReturn(new PaymentsReportPojo(FROM, TO, List.of(), List.of()));
        mockMvc.perform(get("/report/payments").param("dateFrom", "2026-11-01").param("dateTo", "2026-11-11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentMethods").isArray());
    }

    @Test
    void operations_returns200() throws Exception {
        when(reportService.getOperations(any(ReportFilter.class)))
                .thenReturn(new OperationsReportPojo(FROM, TO, List.of(), List.of(), List.of(), List.of()));
        mockMvc.perform(get("/report/operations").param("dateFrom", "2026-11-01").param("dateTo", "2026-11-11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cancellations").isArray());
    }

    @Test
    void export_returns200_withBase64File() throws Exception {
        when(reportService.export(eq("SALES"), any(ReportFilter.class)))
                .thenReturn(new AttachmentPojo("Reporte Ventas.xlsx", "UEsDBA==", AttachmentPojo.TYPE_EXCEL));
        mockMvc.perform(get("/report/export").param("report", "SALES")
                        .param("dateFrom", "2026-11-01").param("dateTo", "2026-11-11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Reporte Ventas.xlsx"))
                .andExpect(jsonPath("$.typeFile").value("EXCEL"));
    }

    @Test
    void export_returns400_whenReportUnknown() throws Exception {
        when(reportService.export(eq("FOO"), any(ReportFilter.class))).thenThrow(new InvalidEnumValueException("Reporte", "FOO"));
        mockMvc.perform(get("/report/export").param("report", "FOO")
                        .param("dateFrom", "2026-11-01").param("dateTo", "2026-11-11"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void export_returns400_whenReportMissing() throws Exception {
        mockMvc.perform(get("/report/export").param("dateFrom", "2026-11-01").param("dateTo", "2026-11-11"))
                .andExpect(status().isBadRequest());
    }
}
