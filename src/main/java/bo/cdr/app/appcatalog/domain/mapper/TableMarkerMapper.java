package bo.cdr.app.appcatalog.domain.mapper;

import bo.cdr.app.appcatalog.domain.entity.TableMarker;
import bo.cdr.app.appcatalog.presentation.request.dto.TableMarkerDto;
import org.springframework.stereotype.Component;

@Component
public class TableMarkerMapper {
    public TableMarker fromDto(TableMarkerDto dto, TableMarker found) {
        TableMarker marker = found != null ? found : new TableMarker();
        marker.setNumber(dto.getNumber());
        marker.setDescription(dto.getDescription());
        marker.setActive(dto.getActive());
        return marker;
    }
}
