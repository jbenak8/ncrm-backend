package cz.jbenak.ncrm_backend.controler;

import cz.jbenak.ncrm_backend.model.dto.order.OrderDto;
import cz.jbenak.ncrm_backend.model.dto.order.OrderRequest;
import cz.jbenak.ncrm_backend.model.entity.order.OrderEntity;
import cz.jbenak.ncrm_backend.security.CustomerScope;
import cz.jbenak.ncrm_backend.services.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-11
 * REST API for realization of customer orders. Creating is allowed to the owner, sales
 * representatives and customers (who order for the customer linked to their account, without
 * a sales representative); updating is allowed to the back office; customers may list their own orders.
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final CustomerScope customerScope;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'SALES_REPRESENTATIVE', 'CUSTOMER')")
    public List<OrderDto> findAll(Authentication authentication) {
        if (customerScope.isCustomer(authentication)) {
            // A customer only sees the orders of the customer record linked to their account.
            return customerScope.customerId(authentication)
                    .map(orderService::findByCustomer)
                    .orElseGet(List::of);
        }
        return orderService.findAll();
    }

    /**
     * Generic search endpoint. Accepts repeatable {@code filter} query parameters in the form
     * {@code field:operator:value} (operators: contains, notContains, eq, neq, lt, gt, between;
     * for between the value is {@code lower,upper}). All filters are combined with AND.
     * Example: {@code /api/orders/search?filter=status:eq:NEW&filter=totalPrice:gt:1000}
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'SALES_REPRESENTATIVE', 'CUSTOMER')")
    public Page<OrderDto> search(@RequestParam(required = false) List<String> filter, Pageable pageable,
                                 Authentication authentication) {
        return orderService.search(customerScope.scopedFilters(filter, authentication), pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'SALES_REPRESENTATIVE', 'CUSTOMER')")
    public OrderDto findById(@PathVariable UUID id) {
        return orderService.findById(id);
    }

    @GetMapping("/by-customer/{customerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'SALES_REPRESENTATIVE', 'CUSTOMER')")
    public List<OrderDto> findByCustomer(@PathVariable UUID customerId) {
        return orderService.findByCustomer(customerId);
    }

    @GetMapping("/by-representative/{salesRepresentativeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'SALES_REPRESENTATIVE')")
    public List<OrderDto> findBySalesRepresentative(@PathVariable UUID salesRepresentativeId) {
        return orderService.findBySalesRepresentative(salesRepresentativeId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'SALES_REPRESENTATIVE', 'CUSTOMER')")
    public OrderDto create(@Valid @RequestBody OrderRequest request, Authentication authentication) {
        return orderService.create(request, authentication == null ? null : authentication.getName());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'SALES_REPRESENTATIVE')")
    public OrderDto update(@PathVariable UUID id, @Valid @RequestBody OrderRequest request) {
        return orderService.update(id, request);
    }

    @PostMapping("/{id}/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'SALES_REPRESENTATIVE')")
    public OrderDto updateStatus(@PathVariable UUID id, @PathVariable OrderEntity.OrderStatus status) {
        return orderService.updateStatus(id, status);
    }
}
