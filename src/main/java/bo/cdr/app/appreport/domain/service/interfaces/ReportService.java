package bo.cdr.app.appreport.domain.service.interfaces;

import bo.cdr.app.appreport.presentation.request.filter.ReportFilter;
import bo.cdr.app.appreport.presentation.response.pojo.AttachmentPojo;
import bo.cdr.app.appreport.presentation.response.pojo.OperationsReportPojo;
import bo.cdr.app.appreport.presentation.response.pojo.PaymentsReportPojo;
import bo.cdr.app.appreport.presentation.response.pojo.ProductsReportPojo;
import bo.cdr.app.appreport.presentation.response.pojo.SalesReportPojo;

/** Reportes de la Etapa 6 (solo ADMIN: ningún otro rol tiene permiso sobre /report). */
public interface ReportService {

    SalesReportPojo getSales(ReportFilter filter);

    ProductsReportPojo getProducts(ReportFilter filter);

    PaymentsReportPojo getPayments(ReportFilter filter);

    OperationsReportPojo getOperations(ReportFilter filter);

    /** Excel (base64) del reporte indicado ({@code ReportTypeEnum}) con los mismos filtros que la pantalla. */
    AttachmentPojo export(String report, ReportFilter filter);
}
