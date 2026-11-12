package bo.cdr.app.appreport.domain.service.implement;

import bo.cdr.app.appreport.domain.service.interfaces.ReportExcelService;
import bo.cdr.app.appreport.presentation.response.pojo.OperationsReportPojo;
import bo.cdr.app.appreport.presentation.response.pojo.PaymentsReportPojo;
import bo.cdr.app.appreport.presentation.response.pojo.ProductsReportPojo;
import bo.cdr.app.appreport.presentation.response.pojo.SalesReportPojo;
import bo.cdr.app.exception.ReportGenerationException;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Excel de reportes con Apache POI (como la referencia). Una hoja por sección: encabezado con el
 * nombre comercial, el rango y la fecha de generación; luego la tabla. Los importes van como número
 * (formato {@code #,##0.00}) para que el usuario pueda sumar/filtrar en Excel.
 */
@Service
public class ReportExcelServiceImpl implements ReportExcelService {

    private static final String BRAND = "Ciudad de Refugio";
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final int MAX_COLUMN_CHARS = 60;
    private static final int ROWS_IN_MEMORY = 200;

    private final Clock clock;

    public ReportExcelServiceImpl(Clock clock) {
        this.clock = clock;
    }

    /** Una tabla de una hoja; {@code totals} es opcional (fila en negrita al final). */
    private record Table(String sheetName, String title, List<String> headers, List<List<Object>> rows, List<Object> totals) {
    }

    @Override
    public byte[] sales(SalesReportPojo r) {
        SalesReportPojo.Summary s = r.summary();
        List<List<Object>> summaryRows = List.of(
                row("Pedidos (sin cancelados)", s.orderCount()),
                row("Bruto", s.gross()),
                row("Descuentos", s.discount()),
                row("Neto", s.net()),
                row("Pagado (cobros vigentes)", s.paid()),
                row("Devuelto (pedidos cancelados)", s.refunded()),
                row("Pedidos cancelados", s.cancelledCount()),
                row("Importe cancelado", s.cancelledAmount()));
        List<List<Object>> periods = r.periods().stream()
                .map(p -> row(p.periodStart(), p.orderCount(), p.gross(), p.discount(), p.net())).toList();
        List<List<Object>> types = r.serviceTypes().stream()
                .map(t -> row(t.serviceTypeLabel(), t.orderCount(), t.net(), t.sharePercent())).toList();
        return write(r.dateFrom(), r.dateTo(),
                new Table("Resumen", "Resumen de ventas", List.of("Concepto", "Valor"), summaryRows, null),
                new Table("Por período", "Ventas por " + r.groupByLabel().toLowerCase(),
                        List.of(r.groupByLabel() + " (inicio)", "Pedidos", "Bruto", "Descuentos", "Neto"), periods,
                        row("Total", s.orderCount(), s.gross(), s.discount(), s.net())),
                new Table("Por modalidad", "Ventas por modalidad", List.of("Modalidad", "Pedidos", "Neto", "% del neto"),
                        types, null));
    }

    @Override
    public byte[] products(ProductsReportPojo r) {
        List<List<Object>> products = r.products().stream()
                .map(p -> row(p.productName(), p.categoryName(), p.quantity(), p.orderCount(), p.gross(), p.sharePercent()))
                .toList();
        List<List<Object>> categories = r.categories().stream()
                .map(c -> row(c.categoryName(), c.quantity(), c.gross(), c.sharePercent())).toList();
        return write(r.dateFrom(), r.dateTo(),
                new Table("Productos", "Productos vendidos (importe antes del descuento general)",
                        List.of("Producto", "Categoría", "Cantidad", "Pedidos", "Importe bruto", "% del bruto"), products,
                        row("Total", "", r.products().stream().mapToLong(ProductsReportPojo.Product::quantity).sum(), "",
                                sumOf(r.products().stream().map(ProductsReportPojo.Product::gross).toList()), "")),
                new Table("Categorías", "Ventas por categoría (importe antes del descuento general)",
                        List.of("Categoría", "Cantidad", "Importe bruto", "% del bruto"), categories, null));
    }

    @Override
    public byte[] payments(PaymentsReportPojo r) {
        List<List<Object>> methods = r.paymentMethods().stream()
                .map(m -> row(m.paymentMethodName(), m.paidCount(), m.paid(), m.refundedCount(), m.refunded(), m.sharePercent()))
                .toList();
        List<List<Object>> sessions = r.cashSessions().stream()
                .map(c -> row(c.id(), c.userFullName(), c.statusLabel(), c.openedAt(), c.closedAt(), c.closedBy(),
                        c.openingAmount(), c.salesTotal(), c.expectedTotal(), c.declaredTotal(), c.differenceTotal()))
                .toList();
        return write(r.dateFrom(), r.dateTo(),
                new Table("Medios de pago", "Cobros por medio de pago",
                        List.of("Medio de pago", "Pagos", "Cobrado", "Devoluciones", "Devuelto", "% de lo cobrado"), methods,
                        row("Total", r.paymentMethods().stream().mapToLong(PaymentsReportPojo.PaymentMethod::paidCount).sum(),
                                sumOf(r.paymentMethods().stream().map(PaymentsReportPojo.PaymentMethod::paid).toList()),
                                r.paymentMethods().stream().mapToLong(PaymentsReportPojo.PaymentMethod::refundedCount).sum(),
                                sumOf(r.paymentMethods().stream().map(PaymentsReportPojo.PaymentMethod::refunded).toList()), "")),
                new Table("Cajas", "Sesiones de caja (abiertas en el rango)",
                        List.of("Caja", "Cajero", "Estado", "Apertura", "Cierre", "Cerrada por", "Monto inicial", "Ventas",
                                "Esperado", "Declarado", "Diferencia"), sessions, null));
    }

    @Override
    public byte[] operations(OperationsReportPojo r) {
        List<List<Object>> users = r.users().stream()
                .map(u -> row(u.userFullName(), u.username(), u.orderCount(), u.gross(), u.discount(), u.net(), u.cancelledCount()))
                .toList();
        List<List<Object>> statuses = r.statuses().stream()
                .map(s -> row(s.statusLabel(), s.orderCount(), s.total())).toList();
        List<List<Object>> discounts = r.discounts().stream()
                .map(d -> row(d.codeLabel(), d.businessDate(), d.cashier(), d.discountTypeLabel(), d.discountValue(),
                        d.subtotal(), d.discountAmount(), d.total(), d.discountReason(), d.appliedBy()))
                .toList();
        List<List<Object>> cancellations = r.cancellations().stream()
                .map(c -> row(c.codeLabel(), c.businessDate(), c.cashier(), c.total(), c.cancelReason(), c.cancelledBy(),
                        c.cancelledAt()))
                .toList();
        return write(r.dateFrom(), r.dateTo(),
                new Table("Cajeros", "Ventas por cajero (sin cancelados)",
                        List.of("Cajero", "Usuario", "Pedidos", "Bruto", "Descuentos", "Neto", "Cancelados"), users, null),
                new Table("Estados", "Pedidos por estado", List.of("Estado", "Pedidos", "Importe"), statuses, null),
                new Table("Descuentos", "Descuentos aplicados",
                        List.of("Pedido", "Fecha", "Cajero", "Tipo", "Valor", "Subtotal", "Descuento", "Total", "Motivo",
                                "Aplicado por"), discounts,
                        row("Total", "", "", "", "", sumOf(r.discounts().stream().map(OperationsReportPojo.DiscountOrder::subtotal).toList()),
                                sumOf(r.discounts().stream().map(OperationsReportPojo.DiscountOrder::discountAmount).toList()),
                                sumOf(r.discounts().stream().map(OperationsReportPojo.DiscountOrder::total).toList()), "", "")),
                new Table("Cancelaciones", "Pedidos cancelados",
                        List.of("Pedido", "Fecha", "Cajero", "Total", "Motivo", "Cancelado por", "Fecha de cancelación"),
                        cancellations, null));
    }

    // ------------------------------------------------------------------ escritura

    /**
     * SXSSF (Etapa 7): escribe por bloques de {@value #ROWS_IN_MEMORY} filas en lugar de mantener todo el
     * libro en memoria; con un año de descuentos/cancelaciones el XSSF completo era el cuello de botella.
     */
    private byte[] write(LocalDate from, LocalDate to, Table... tables) {
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(ROWS_IN_MEMORY); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Styles styles = new Styles(workbook);
            String range = "Período: " + DATE.format(from) + " al " + DATE.format(to);
            String generated = "Generado: " + DATE_TIME.format(LocalDateTime.now(clock));
            for (Table table : tables) {
                writeSheet(workbook.createSheet(table.sheetName()), styles, table, range, generated);
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new ReportGenerationException("No se pudo generar el archivo Excel.", e);
        }
    }

    private void writeSheet(Sheet sheet, Styles styles, Table table, String range, String generated) {
        int[] widths = new int[table.headers().size()];
        int rowIndex = 0;
        setText(sheet.createRow(rowIndex++), 0, BRAND + " — " + table.title(), styles.title);
        setText(sheet.createRow(rowIndex++), 0, range, null);
        setText(sheet.createRow(rowIndex++), 0, generated, null);
        rowIndex++;

        Row header = sheet.createRow(rowIndex++);
        for (int i = 0; i < table.headers().size(); i++) {
            setText(header, i, table.headers().get(i), styles.header);
            widths[i] = table.headers().get(i).length();
        }
        int firstDataRow = rowIndex;
        for (List<Object> values : table.rows()) {
            writeRow(sheet.createRow(rowIndex++), values, styles, false, widths);
        }
        if (table.rows().isEmpty()) {
            setText(sheet.createRow(rowIndex++), 0, "Sin datos en el período.", null);
        } else if (table.totals() != null) {
            writeRow(sheet.createRow(rowIndex), table.totals(), styles, true, widths);
        }
        sheet.createFreezePane(0, firstDataRow);
        // Ancho por contenido (sin autoSizeColumn: evita depender de fuentes AWT en el servidor)
        for (int i = 0; i < widths.length; i++) {
            sheet.setColumnWidth(i, (Math.min(widths[i], MAX_COLUMN_CHARS) + 3) * 256);
        }
    }

    private void writeRow(Row row, List<Object> values, Styles styles, boolean bold, int[] widths) {
        for (int i = 0; i < values.size(); i++) {
            Object value = values.get(i);
            Cell cell = row.createCell(i);
            String shown;
            switch (value) {
                case null -> {
                    shown = "";
                    cell.setCellStyle(bold ? styles.textBold : styles.text);
                }
                case BigDecimal amount -> {
                    cell.setCellValue(amount.doubleValue());
                    cell.setCellStyle(bold ? styles.moneyBold : styles.money);
                    shown = amount.toPlainString() + "   ";
                }
                case Number number -> {
                    cell.setCellValue(number.doubleValue());
                    cell.setCellStyle(bold ? styles.integerBold : styles.integer);
                    shown = number.toString();
                }
                case LocalDateTime dateTime -> {
                    shown = DATE_TIME.format(dateTime);
                    cell.setCellValue(shown);
                    cell.setCellStyle(styles.text);
                }
                case LocalDate date -> {
                    shown = DATE.format(date);
                    cell.setCellValue(shown);
                    cell.setCellStyle(styles.text);
                }
                default -> {
                    shown = value.toString();
                    cell.setCellValue(shown);
                    cell.setCellStyle(bold ? styles.textBold : styles.text);
                }
            }
            if (i < widths.length) {
                widths[i] = Math.max(widths[i], shown.length());
            }
        }
    }

    private static void setText(Row row, int column, String text, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(text);
        if (style != null) {
            cell.setCellStyle(style);
        }
    }

    private static List<Object> row(Object... values) {
        return new ArrayList<>(Arrays.asList(values));
    }

    private static BigDecimal sumOf(List<BigDecimal> amounts) {
        return amounts.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Estilos creados una vez por libro (POI limita la cantidad de estilos). */
    private static final class Styles {
        final CellStyle title;
        final CellStyle header;
        final CellStyle text;
        final CellStyle textBold;
        final CellStyle money;
        final CellStyle moneyBold;
        final CellStyle integer;
        final CellStyle integerBold;

        Styles(Workbook workbook) {
            DataFormat format = workbook.createDataFormat();
            Font boldFont = workbook.createFont();
            boldFont.setBold(true);
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());

            title = workbook.createCellStyle();
            title.setFont(titleFont);

            header = workbook.createCellStyle();
            header.setFont(headerFont);
            header.setFillForegroundColor(IndexedColors.TEAL.getIndex());
            header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            bordered(header);

            text = bordered(workbook.createCellStyle());
            textBold = bordered(workbook.createCellStyle());
            textBold.setFont(boldFont);

            money = bordered(workbook.createCellStyle());
            money.setDataFormat(format.getFormat("#,##0.00"));
            moneyBold = bordered(workbook.createCellStyle());
            moneyBold.setDataFormat(format.getFormat("#,##0.00"));
            moneyBold.setFont(boldFont);

            integer = bordered(workbook.createCellStyle());
            integer.setDataFormat(format.getFormat("0"));
            integerBold = bordered(workbook.createCellStyle());
            integerBold.setDataFormat(format.getFormat("0"));
            integerBold.setFont(boldFont);
        }

        private static CellStyle bordered(CellStyle style) {
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderTop(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);
            return style;
        }
    }
}
