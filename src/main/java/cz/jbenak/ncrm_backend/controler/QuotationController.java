package cz.jbenak.ncrm_backend.controler;

import cz.jbenak.ncrm_backend.model.dto.quotation.QuotationDto;
import cz.jbenak.ncrm_backend.model.dto.quotation.QuotationRequest;
import cz.jbenak.ncrm_backend.model.entity.quotation.QuotationEntity;
import cz.jbenak.ncrm_backend.services.QuotationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-22
 * REST API for realization of price quotations. Creating and updating is allowed to the owner
 * and sales representatives; customers may list their own quotations. A quotation can be sent
 * to the customer by e-mail and converted into an order.
 */
@RestController
@RequestMapping("/api/quotations")
@RequiredArgsConstructor
public class QuotationController {

    private final QuotationService quotationService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'SALES_REPRESENTATIVE')")
    public List<QuotationDto> findAll() {
        return quotationService.findAll();
    }

    /**
     * Generic search endpoint. Accepts repeatable {@code filter} query parameters in the form
     * {@code field:operator:value} (operators: contains, notContains, eq, neq, lt, gt, between;
     * for between the value is {@code lower,upper}). All filters are combined with AND.
     * Example: {@code /api/quotations/search?filter=status:eq:NEW&filter=totalPrice:gt:1000}
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'SALES_REPRESENTATIVE')")
    public Page<QuotationDto> search(@RequestParam(required = false) List<String> filter, Pageable pageable) {
        return quotationService.search(filter, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'SALES_REPRESENTATIVE', 'CUSTOMER')")
    public QuotationDto findById(@PathVariable UUID id) {
        return quotationService.findById(id);
    }

    @GetMapping("/by-customer/{customerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'SALES_REPRESENTATIVE', 'CUSTOMER')")
    public List<QuotationDto> findByCustomer(@PathVariable UUID customerId) {
        return quotationService.findByCustomer(customerId);
    }

    @GetMapping("/by-representative/{salesRepresentativeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'SALES_REPRESENTATIVE')")
    public List<QuotationDto> findBySalesRepresentative(@PathVariable UUID salesRepresentativeId) {
        return quotationService.findBySalesRepresentative(salesRepresentativeId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'SALES_REPRESENTATIVE')")
    public QuotationDto create(@Valid @RequestBody QuotationRequest request) {
        return quotationService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'SALES_REPRESENTATIVE')")
    public QuotationDto update(@PathVariable UUID id, @Valid @RequestBody QuotationRequest request) {
        return quotationService.update(id, request);
    }

    @PostMapping("/{id}/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'SALES_REPRESENTATIVE')")
    public QuotationDto updateStatus(@PathVariable UUID id, @PathVariable QuotationEntity.QuotationStatus status) {
        return quotationService.updateStatus(id, status);
    }

    /** Sends the quotation to the customer by e-mail. */
    @PostMapping("/{id}/send-email")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'SALES_REPRESENTATIVE')")
    public QuotationDto sendToCustomer(@PathVariable UUID id) {
        return quotationService.sendToCustomer(id);
    }

    /** Converts the quotation into a customer order with the quoted prices. */
    @PostMapping("/{id}/create-order")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'SALES_REPRESENTATIVE')")
    public QuotationDto createOrder(@PathVariable UUID id) {
        return quotationService.createOrder(id);
    }
}
