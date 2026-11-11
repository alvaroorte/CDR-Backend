package bo.cdr.app.appcatalog.domain.service.implement;

import bo.cdr.app.appbase.data.repository.GenericRepository;
import bo.cdr.app.appbase.domain.service.CRUDServiceImpl;
import bo.cdr.app.appcatalog.data.TableMarkerRepository;
import bo.cdr.app.appcatalog.domain.entity.TableMarker;
import bo.cdr.app.appcatalog.domain.mapper.TableMarkerMapper;
import bo.cdr.app.appcatalog.domain.service.interfaces.TableMarkerService;
import bo.cdr.app.appcatalog.presentation.request.dto.TableMarkerDto;
import bo.cdr.app.appcatalog.presentation.response.pojo.TableMarkerAvailabilityPojo;
import bo.cdr.app.appcatalog.presentation.response.pojo.TableMarkerPojo;
import bo.cdr.app.common.enums.OrderStatusEnum;
import bo.cdr.app.exception.DuplicateException;
import bo.cdr.app.exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** La disponibilidad (libre/en uso) se deriva de los pedidos abiertos (getAvailability). */
@Service
@RequiredArgsConstructor
public class TableMarkerServiceImpl extends CRUDServiceImpl<TableMarker, Integer> implements TableMarkerService {

    private final TableMarkerRepository tableMarkerRepository;
    private final TableMarkerMapper tableMarkerMapper;

    @Override
    protected GenericRepository<TableMarker, Integer> getRepository() {
        return tableMarkerRepository;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TableMarker create(TableMarkerDto dto) {
        validateDuplicate(dto, null);
        return tableMarkerRepository.save(tableMarkerMapper.fromDto(dto, null));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TableMarker update(Integer id, TableMarkerDto dto) {
        TableMarker found = getById(id);
        validateDuplicate(dto, id);
        return tableMarkerRepository.save(tableMarkerMapper.fromDto(dto, found));
    }

    @Override
    public List<TableMarkerPojo> getAllTableMarkerPojo() {
        return tableMarkerRepository.findAllTableMarkerPojo();
    }

    @Override
    public TableMarkerPojo getTableMarkerPojoById(Integer id) {
        return tableMarkerRepository.getTableMarkerPojoById(id)
                .orElseThrow(() -> new EntityNotFoundException("Número de mesa", id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TableMarkerAvailabilityPojo> getAvailability() {
        return tableMarkerRepository.findActiveWithOpenOrder(OrderStatusEnum.OPEN_STATUSES).stream()
                .map(row -> new TableMarkerAvailabilityPojo(row.id(), row.number(), row.description(),
                        row.orderId() != null, row.orderId(),
                        row.orderCode() == null ? null : String.format("#%03d", row.orderCode())))
                .toList();
    }

    private void validateDuplicate(TableMarkerDto dto, Integer excludeId) {
        if (!Boolean.TRUE.equals(dto.getActive())) {
            return;
        }
        boolean exists = excludeId == null
                ? tableMarkerRepository.existsByNumberAndActiveIsTrue(dto.getNumber())
                : tableMarkerRepository.existsByNumberAndActiveIsTrueAndIdNot(dto.getNumber(), excludeId);
        if (exists) {
            throw new DuplicateException("Número de mesa", "Número", String.valueOf(dto.getNumber()));
        }
    }
}
