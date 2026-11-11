package bo.cdr.app.apporder.presentation.controller;

import bo.cdr.app.appbase.presentation.response.PagePojo;
import bo.cdr.app.apporder.domain.service.interfaces.OrderService;
import bo.cdr.app.apporder.presentation.request.dto.OrderCancelDto;
import bo.cdr.app.apporder.presentation.request.dto.OrderDto;
import bo.cdr.app.apporder.presentation.request.dto.OrderQuoteDto;
import bo.cdr.app.apporder.presentation.request.dto.OrderStatusDto;
import bo.cdr.app.apporder.presentation.request.filter.OrderFilter;
import bo.cdr.app.apporder.presentation.response.pojo.KitchenSettingsPojo;
import bo.cdr.app.apporder.presentation.response.pojo.OrderBoardPojo;
import bo.cdr.app.apporder.presentation.response.pojo.OrderListPojo;
import bo.cdr.app.apporder.presentation.response.pojo.OrderPojo;
import bo.cdr.app.apporder.presentation.response.pojo.OrderQuotePojo;
import bo.cdr.app.common.enums.OrderStatusEnum;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Pedidos: cotización y registro (Etapa 3); tablero, estados, cancelación e historial (Etapa 4).
 * Quién puede hacer cada transición lo decide el servicio (OrderStateMachine), no solo el permiso de URL.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/order")
public class OrderController {

    private final OrderService orderService;

    /** Resumen calculado por el backend para la confirmación del POS (no persiste). */
    @PostMapping("/quote")
    public ResponseEntity<OrderQuotePojo> quote(@Valid @RequestBody OrderQuoteDto dto) {
        return ResponseEntity.status(HttpStatus.OK).body(orderService.quote(dto));
    }

    @PostMapping
    public ResponseEntity<OrderPojo> create(@Valid @RequestBody OrderDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.create(dto));
    }

    /** Fuente de verdad del tablero al conectar/reconectar el WebSocket. Sin statuses = pedidos abiertos. */
    @GetMapping("/board")
    public ResponseEntity<List<OrderBoardPojo>> getBoard(@RequestParam(required = false) List<OrderStatusEnum> statuses) {
        return ResponseEntity.status(HttpStatus.OK).body(orderService.getBoard(statuses));
    }

    @GetMapping("/kitchen-settings")
    public ResponseEntity<KitchenSettingsPojo> getKitchenSettings() {
        return ResponseEntity.status(HttpStatus.OK).body(orderService.getKitchenSettings());
    }

    @GetMapping("/pageable")
    public ResponseEntity<PagePojo<OrderListPojo>> pageable(@RequestParam(defaultValue = "0") int page,
                                                             @RequestParam(defaultValue = "10") int size,
                                                             @RequestParam(required = false) String sortBy,
                                                             @RequestParam(required = false) String sortOrder,
                                                             @ModelAttribute OrderFilter filter) {
        return ResponseEntity.status(HttpStatus.OK).body(orderService.pageable(page, size, sortBy, sortOrder, filter));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderPojo> getById(@PathVariable Integer id) {
        return ResponseEntity.status(HttpStatus.OK).body(orderService.getOrderPojoById(id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderPojo> changeStatus(@PathVariable Integer id, @Valid @RequestBody OrderStatusDto dto) {
        return ResponseEntity.status(HttpStatus.OK).body(orderService.changeStatus(id, dto));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<OrderPojo> cancel(@PathVariable Integer id, @Valid @RequestBody OrderCancelDto dto) {
        return ResponseEntity.status(HttpStatus.OK).body(orderService.cancel(id, dto));
    }
}
