package bo.cdr.app.appreport.domain.service.interfaces;

import bo.cdr.app.appreport.presentation.response.pojo.OperationsReportPojo;
import bo.cdr.app.appreport.presentation.response.pojo.PaymentsReportPojo;
import bo.cdr.app.appreport.presentation.response.pojo.ProductsReportPojo;
import bo.cdr.app.appreport.presentation.response.pojo.SalesReportPojo;

/** Genera el .xlsx (Apache POI) a partir de los datos ya calculados por {@link ReportService}. */
public interface ReportExcelService {

    byte[] sales(SalesReportPojo report);

    byte[] products(ProductsReportPojo report);

    byte[] payments(PaymentsReportPojo report);

    byte[] operations(OperationsReportPojo report);
}
