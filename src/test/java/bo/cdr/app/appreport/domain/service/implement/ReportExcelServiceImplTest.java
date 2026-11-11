package bo.cdr.app.appreport.domain.service.implement;

import bo.cdr.app.appreport.presentation.response.pojo.OperationsReportPojo;
import bo.cdr.app.appreport.presentation.response.pojo.PaymentsReportPojo;
import bo.cdr.app.appreport.presentation.response.pojo.ProductsReportPojo;
import bo.cdr.app.appreport.presentation.response.pojo.SalesReportPojo;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReportExcelServiceImplTest {

    private static final LocalDate FROM = LocalDate.of(2026, 11, 1);
    private static final LocalDate TO = LocalDate.of(2026, 11, 11);
    /** Encabezado: título, período, generado, línea en blanco → cabecera de tabla en la fila 4. */
    private static final int HEADER_ROW = 4;

    private final ReportExcelServiceImpl service = new ReportExcelServiceImpl(
            Clock.fixed(Instant.parse("2026-11-11T14:30:00Z"), ZoneId.of("America/La_Paz")));

    private static XSSFWorkbook read(byte[] content) throws IOException {
        return new XSSFWorkbook(new ByteArrayInputStream(content));
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    @Test
    void sales_writesSummaryPeriodsAndServiceTypes_withNumericAmounts() throws IOException {
        SalesReportPojo report = new SalesReportPojo(FROM, TO, "DAY", "Día",
                new SalesReportPojo.Summary(2, bd("31.00"), bd("1.00"), bd("30.00"), bd("30.00"), bd("0.00"),
                        bd("15.00"), 1, bd("15.50")),
                List.of(new SalesReportPojo.Period(FROM, 2, bd("31.00"), bd("1.00"), bd("30.00"))),
                List.of(new SalesReportPojo.ServiceType("TAKEAWAY", "Para llevar", 2, bd("30.00"), bd("100.00"))));

        try (XSSFWorkbook workbook = read(service.sales(report))) {
            assertThat(workbook.getNumberOfSheets()).isEqualTo(3);
            assertThat(workbook.getSheetName(0)).isEqualTo("Resumen");
            Sheet summary = workbook.getSheet("Resumen");
            assertThat(summary.getRow(0).getCell(0).getStringCellValue()).contains("Ciudad de Refugio");
            assertThat(summary.getRow(1).getCell(0).getStringCellValue()).isEqualTo("Período: 01/11/2026 al 11/11/2026");
            assertThat(summary.getRow(2).getCell(0).getStringCellValue()).isEqualTo("Generado: 11/11/2026 10:30");
            Row net = summary.getRow(HEADER_ROW + 4);
            assertThat(net.getCell(0).getStringCellValue()).isEqualTo("Neto");
            assertThat(net.getCell(1).getCellType()).isEqualTo(CellType.NUMERIC);
            assertThat(net.getCell(1).getNumericCellValue()).isEqualTo(30.0);

            Sheet periods = workbook.getSheet("Por período");
            assertThat(periods.getRow(HEADER_ROW).getCell(0).getStringCellValue()).isEqualTo("Día (inicio)");
            assertThat(periods.getRow(HEADER_ROW + 1).getCell(0).getStringCellValue()).isEqualTo("01/11/2026");
            Row total = periods.getRow(HEADER_ROW + 2);
            assertThat(total.getCell(0).getStringCellValue()).isEqualTo("Total");
            assertThat(total.getCell(4).getNumericCellValue()).isEqualTo(30.0);
        }
    }

    @Test
    void products_writesTotalsRow_andEmptyMessageWhenNoCategories() throws IOException {
        ProductsReportPojo report = new ProductsReportPojo(FROM, TO, List.of(
                new ProductsReportPojo.Product(1, "Capuchino", "Bebidas", 3, 2, bd("46.50"), bd("75.00")),
                new ProductsReportPojo.Product(2, "Latte", "Bebidas", 1, 1, bd("15.50"), bd("25.00"))), List.of());

        try (XSSFWorkbook workbook = read(service.products(report))) {
            Sheet products = workbook.getSheet("Productos");
            assertThat(products.getRow(HEADER_ROW + 1).getCell(0).getStringCellValue()).isEqualTo("Capuchino");
            Row total = products.getRow(HEADER_ROW + 3);
            assertThat(total.getCell(2).getNumericCellValue()).isEqualTo(4.0);
            assertThat(total.getCell(4).getNumericCellValue()).isEqualTo(62.0);
            Sheet categories = workbook.getSheet("Categorías");
            assertThat(categories.getRow(HEADER_ROW + 1).getCell(0).getStringCellValue()).isEqualTo("Sin datos en el período.");
        }
    }

    @Test
    void payments_leavesBlankCloseTotals_forOpenSessions() throws IOException {
        PaymentsReportPojo report = new PaymentsReportPojo(FROM, TO,
                List.of(new PaymentsReportPojo.PaymentMethod(1, "Efectivo", 2, bd("31.00"), 1, bd("15.50"), bd("100.00"))),
                List.of(new PaymentsReportPojo.CashSession(7, "maria.caja", "María Caja", "OPEN", "Abierta",
                        LocalDateTime.of(2026, 11, 11, 8, 0), null, null, bd("100.00"), bd("31.00"), null, null, null)));

        try (XSSFWorkbook workbook = read(service.payments(report))) {
            Row session = workbook.getSheet("Cajas").getRow(HEADER_ROW + 1);
            assertThat(session.getCell(1).getStringCellValue()).isEqualTo("María Caja");
            assertThat(session.getCell(3).getStringCellValue()).isEqualTo("11/11/2026 08:00");
            assertThat(session.getCell(8).getStringCellValue()).isEmpty();
            Row methodTotal = workbook.getSheet("Medios de pago").getRow(HEADER_ROW + 2);
            assertThat(methodTotal.getCell(4).getNumericCellValue()).isEqualTo(15.5);
        }
    }

    @Test
    void operations_writesFourSheets() throws IOException {
        OperationsReportPojo report = new OperationsReportPojo(FROM, TO,
                List.of(new OperationsReportPojo.UserSales(4, "maria.caja", "María Caja", 2, bd("31.00"), bd("1.00"),
                        bd("30.00"), 1)),
                List.of(new OperationsReportPojo.StatusCount("DELIVERED", "ENTREGADO", 2, bd("30.00"))),
                List.of(new OperationsReportPojo.DiscountOrder(10, "#005", FROM, LocalDateTime.of(2026, 11, 1, 9, 0),
                        "María Caja", "FIXED_AMOUNT", "Monto fijo", bd("1.00"), bd("16.50"), bd("1.00"), bd("15.50"),
                        "Cliente frecuente", "maria.caja")),
                List.of());

        try (XSSFWorkbook workbook = read(service.operations(report))) {
            assertThat(workbook.getNumberOfSheets()).isEqualTo(4);
            Row discount = workbook.getSheet("Descuentos").getRow(HEADER_ROW + 1);
            assertThat(discount.getCell(0).getStringCellValue()).isEqualTo("#005");
            assertThat(discount.getCell(8).getStringCellValue()).isEqualTo("Cliente frecuente");
            assertThat(workbook.getSheet("Cancelaciones").getRow(HEADER_ROW + 1).getCell(0).getStringCellValue())
                    .isEqualTo("Sin datos en el período.");
        }
    }
}
