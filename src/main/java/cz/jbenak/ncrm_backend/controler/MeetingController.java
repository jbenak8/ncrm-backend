package cz.jbenak.ncrm_backend.controler;

import cz.jbenak.ncrm_backend.model.dto.customer.MeetingDto;
import cz.jbenak.ncrm_backend.model.dto.customer.MeetingRequest;
import cz.jbenak.ncrm_backend.services.MeetingService;
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
 * REST API for planning and realization of business meetings including meeting minutes.
 * Available to the owner and sales representatives.
 */
@RestController
@RequestMapping("/api/meetings")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('OWNER', 'SALES_REPRESENTATIVE')")
public class MeetingController {

    private final MeetingService meetingService;

    @GetMapping
    public List<MeetingDto> findAll() {
        return meetingService.findAll();
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
