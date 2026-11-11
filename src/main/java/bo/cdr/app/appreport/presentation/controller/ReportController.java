package bo.cdr.app.appreport.presentation.controller;

import bo.cdr.app.appreport.domain.service.interfaces.ReportService;
import bo.cdr.app.appreport.presentation.request.filter.ReportFilter;
import bo.cdr.app.appreport.presentation.response.pojo.AttachmentPojo;
import bo.cdr.app.appreport.presentation.response.pojo.OperationsReportPojo;
import bo.cdr.app.appreport.presentation.response.pojo.PaymentsReportPojo;
import bo.cdr.app.appreport.presentation.response.pojo.ProductsReportPojo;
import bo.cdr.app.appreport.presentation.response.pojo.SalesReportPojo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Reportes (Etapa 6). Solo ADMIN: ninguna migración asigna permisos de /report a otros roles y el
 * {@code AuthorizationFilter} deja pasar al ADMIN sin permisos explícitos.
 * Rango de fechas obligatorio ({@code dateFrom}, {@code dateTo}, ISO yyyy-MM-dd).
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/report")
public class ReportController {

    private final ReportService reportService;

    /** Resumen Bruto / Descuentos / Neto / Pagado, por período ({@code groupBy}=DAY|WEEK|MONTH) y por modalidad. */
    @GetMapping("/sales")
    public ResponseEntity<SalesReportPojo> sales(@ModelAttribute ReportFilter filter) {
        return ResponseEntity.status(HttpStatus.OK).body(reportService.getSales(filter));
    }

    @GetMapping("/products")
    public ResponseEntity<ProductsReportPojo> products(@ModelAttribute ReportFilter filter) {
        return ResponseEntity.status(HttpStatus.OK).body(reportService.getProducts(filter));
    }

    @GetMapping("/payments")
    public ResponseEntity<PaymentsReportPojo> payments(@ModelAttribute ReportFilter filter) {
        return ResponseEntity.status(HttpStatus.OK).body(reportService.getPayments(filter));
    }

    @GetMapping("/operations")
    public ResponseEntity<OperationsReportPojo> operations(@ModelAttribute ReportFilter filter) {
        return ResponseEntity.status(HttpStatus.OK).body(reportService.getOperations(filter));
    }

    /**
     * Excel en base64 ({@code report} = SALES | PRODUCTS | PAYMENTS | OPERATIONS). El tipo va como query
     * param y no en la ruta: el código de permiso se arma con la URL sin ids numéricos.
     */
    @GetMapping("/export")
    public ResponseEntity<AttachmentPojo> export(@RequestParam String report, @ModelAttribute ReportFilter filter) {
        return ResponseEntity.status(HttpStatus.OK).body(reportService.export(report, filter));
    }
}
