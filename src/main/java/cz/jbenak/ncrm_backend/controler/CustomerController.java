package cz.jbenak.ncrm_backend.controler;

import cz.jbenak.ncrm_backend.model.dto.customer.CustomerDto;
import cz.jbenak.ncrm_backend.model.dto.customer.CustomerRequest;
import cz.jbenak.ncrm_backend.services.CustomerService;
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
 * @since 2026-07-11
 * REST API for customer management. Reading and editing is allowed to the owner and sales representatives;
 * deactivation is restricted to the owner.
 */
@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'SALES_REPRESENTATIVE')")
    public Page<CustomerDto> findAll(@RequestParam(required = false) String name, Pageable pageable) {
        return customerService.findAll(name, pageable);
    }

    /**
     * Generic search endpoint. Accepts repeatable {@code filter} query parameters in the form
     * {@code field:operator:value} (operators: contains, notContains, eq, neq, lt, gt, between;
     * for between the value is {@code lower,upper}). All filters are combined with AND.
     * Example: {@code /api/customers/search?filter=name:contains:teza&filter=active:eq:true}
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'SALES_REPRESENTATIVE')")
    public Page<CustomerDto> search(@RequestParam(required = false) List<String> filter, Pageable pageable) {
        return customerService.search(filter, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'SALES_REPRESENTATIVE')")
    public CustomerDto findById(@PathVariable UUID id) {
        return customerService.findById(id);
    }

    @GetMapping("/by-representative/{salesRepresentativeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'SALES_REPRESENTATIVE')")
    public List<CustomerDto> findBySalesRepresentative(@PathVariable UUID salesRepresentativeId) {
        return customerService.findBySalesRepresentative(salesRepresentativeId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'SALES_REPRESENTATIVE')")
    public CustomerDto create(@Valid @RequestBody CustomerRequest request) {
        return customerService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'SALES_REPRESENTATIVE')")
    public CustomerDto update(@PathVariable UUID id, @Valid @RequestBody CustomerRequest request) {
        return customerService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public void deactivate(@PathVariable UUID id) {
        customerService.deactivate(id);
    }
}
