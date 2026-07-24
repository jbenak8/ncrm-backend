package cz.jbenak.ncrm_backend.controler;

import cz.jbenak.ncrm_backend.model.dto.customer.MeetingDto;
import cz.jbenak.ncrm_backend.model.dto.customer.MeetingRequest;
import cz.jbenak.ncrm_backend.security.CustomerScope;
import cz.jbenak.ncrm_backend.services.MeetingService;
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
 * REST API for planning and realization of business meetings including meeting minutes.
 * Available to the owner and sales representatives; customers may list their own meetings.
 */
@RestController
@RequestMapping("/api/meetings")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'SALES_REPRESENTATIVE')")
public class MeetingController {

    private final MeetingService meetingService;
    private final CustomerScope customerScope;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'SALES_REPRESENTATIVE', 'CUSTOMER')")
    public List<MeetingDto> findAll(Authentication authentication) {
        if (customerScope.isCustomer(authentication)) {
            // A customer only sees the meetings of the customer record linked to their account.
            return customerScope.customerId(authentication)
                    .map(meetingService::findByCustomer)
                    .orElseGet(List::of);
        }
        return meetingService.findAll();
    }

    /**
     * Generic search endpoint. Accepts repeatable {@code filter} query parameters in the form
     * {@code field:operator:value} (operators: contains, notContains, eq, neq, lt, gt, between;
     * for between the value is {@code lower,upper}). All filters are combined with AND.
     * Example: {@code /api/meetings/search?filter=subject:contains:demo&filter=status:eq:PLANNED}
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'SALES_REPRESENTATIVE', 'CUSTOMER')")
    public Page<MeetingDto> search(@RequestParam(required = false) List<String> filter, Pageable pageable,
                                   Authentication authentication) {
        return meetingService.search(customerScope.scopedFilters(filter, authentication), pageable);
    }

    @GetMapping("/{id}")
    public MeetingDto findById(@PathVariable UUID id) {
        return meetingService.findById(id);
    }

    @GetMapping("/by-customer/{customerId}")
    public List<MeetingDto> findByCustomer(@PathVariable UUID customerId) {
        return meetingService.findByCustomer(customerId);
    }

    @GetMapping("/by-representative/{salesRepresentativeId}")
    public List<MeetingDto> findBySalesRepresentative(@PathVariable UUID salesRepresentativeId) {
        return meetingService.findBySalesRepresentative(salesRepresentativeId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MeetingDto create(@Valid @RequestBody MeetingRequest request) {
        return meetingService.create(request);
    }

    @PutMapping("/{id}")
    public MeetingDto update(@PathVariable UUID id, @Valid @RequestBody MeetingRequest request) {
        return meetingService.update(id, request);
    }

    /**
     * Completes the meeting and stores the meeting minutes (outcome).
     */
    @PostMapping("/{id}/complete")
    public MeetingDto complete(@PathVariable UUID id, @RequestBody String outcome) {
        return meetingService.complete(id, outcome);
    }

    @PostMapping("/{id}/cancel")
    public MeetingDto cancel(@PathVariable UUID id) {
        return meetingService.cancel(id);
    }
}
