package cz.jbenak.ncrm_backend.controler;

import cz.jbenak.ncrm_backend.model.dto.order.OrderDto;
import cz.jbenak.ncrm_backend.model.dto.order.OrderRequest;
import cz.jbenak.ncrm_backend.model.entity.order.OrderEntity;
import cz.jbenak.ncrm_backend.services.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-11
 * REST API for realization of customer orders. Creating and updating is allowed to the owner
 * and sales representatives; customers may list their own orders.
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'SALES_REPRESENTATIVE')")
    public List<OrderDto> findAll() {
        return orderService.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'SALES_REPRESENTATIVE', 'CUSTOMER')")
    public OrderDto findById(@PathVariable UUID id) {
        return orderService.findById(id);
    }

    @GetMapping("/by-customer/{customerId}")
    @PreAuthorize("hasAnyRole('OWNER', 'SALES_REPRESENTATIVE', 'CUSTOMER')")
    public List<OrderDto> findByCustomer(@PathVariable UUID customerId) {
        return orderService.findByCustomer(customerId);
    }

    @GetMapping("/by-representative/{salesRepresentativeId}")
    @PreAuthorize("hasAnyRole('OWNER', 'SALES_REPRESENTATIVE')")
    public List<OrderDto> findBySalesRepresentative(@PathVariable UUID salesRepresentativeId) {
        return orderService.findBySalesRepresentative(salesRepresentativeId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('OWNER', 'SALES_REPRESENTATIVE')")
    public OrderDto create(@Valid @RequestBody OrderRequest request) {
        return orderService.create(request);
    }

    @PostMapping("/{id}/status/{status}")
    @PreAuthorize("hasAnyRole('OWNER', 'SALES_REPRESENTATIVE')")
    public OrderDto updateStatus(@PathVariable UUID id, @PathVariable OrderEntity.OrderStatus status) {
        return orderService.updateStatus(id, status);
    }
}
