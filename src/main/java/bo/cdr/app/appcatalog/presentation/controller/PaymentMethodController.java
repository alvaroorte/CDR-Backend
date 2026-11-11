package bo.cdr.app.appcatalog.presentation.controller;

import bo.cdr.app.appcatalog.domain.entity.PaymentMethod;
import bo.cdr.app.appcatalog.domain.service.interfaces.PaymentMethodService;
import bo.cdr.app.appcatalog.presentation.request.dto.PaymentMethodDto;
import bo.cdr.app.appcatalog.presentation.response.pojo.PaymentMethodPojo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/payment-method")
public class PaymentMethodController {

    private final PaymentMethodService paymentMethodService;

    @GetMapping
    public ResponseEntity<List<PaymentMethodPojo>> getAll() {
        return ResponseEntity.status(HttpStatus.OK).body(paymentMethodService.getAllPaymentMethodPojo());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentMethodPojo> getById(@PathVariable Integer id) {
        return ResponseEntity.status(HttpStatus.OK).body(paymentMethodService.getPaymentMethodPojoById(id));
    }

    @PostMapping
    public ResponseEntity<PaymentMethod> create(@Valid @RequestBody PaymentMethodDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentMethodService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PaymentMethod> update(@PathVariable Integer id, @Valid @RequestBody PaymentMethodDto dto) {
        return ResponseEntity.status(HttpStatus.OK).body(paymentMethodService.update(id, dto));
    }

    /** Borrado lógico (desactiva). */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        paymentMethodService.softDelete(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
