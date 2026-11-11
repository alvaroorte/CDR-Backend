package bo.cdr.app.appcatalog.domain.service.interfaces;

import bo.cdr.app.appbase.domain.service.CRUDService;
import bo.cdr.app.appcatalog.domain.entity.TableMarker;
import bo.cdr.app.appcatalog.presentation.request.dto.TableMarkerDto;
import bo.cdr.app.appcatalog.presentation.response.pojo.TableMarkerAvailabilityPojo;
import bo.cdr.app.appcatalog.presentation.response.pojo.TableMarkerPojo;

import java.util.List;

public interface TableMarkerService extends CRUDService<TableMarker, Integer> {
    TableMarker create(TableMarkerDto dto);
    TableMarker update(Integer id, TableMarkerDto dto);
    List<TableMarkerPojo> getAllTableMarkerPojo();
    TableMarkerPojo getTableMarkerPojoById(Integer id);
    /** Adornos activos con ocupación derivada de los pedidos abiertos (POS). */
    List<TableMarkerAvailabilityPojo> getAvailability();
}
