package cz.jbenak.ncrm_backend.controler;

import cz.jbenak.ncrm_backend.model.dto.company.SalesRepresentativeDto;
import cz.jbenak.ncrm_backend.model.dto.company.SalesRepresentativeRequest;
import cz.jbenak.ncrm_backend.services.SalesRepresentativeService;
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
 * @since 2026-07-13
 * REST API for administration of sales representatives. Listing including inactive representatives
 * and all modifications are restricted to the owner; active representatives are also available
 * on /api/users/sales-representatives for the sales team.
 */
@RestController
@RequestMapping("/api/sales-representatives")
@RequiredArgsConstructor
public class SalesRepresentativeController {

    private final SalesRepresentativeService salesRepresentativeService;

    @GetMapping
    @PreAuthorize("hasRole('OWNER')")
    public List<SalesRepresentativeDto> findAll() {
        return salesRepresentativeService.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'SALES_REPRESENTATIVE')")
    public SalesRepresentativeDto findById(@PathVariable UUID id) {
        return salesRepresentativeService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('OWNER')")
    public SalesRepresentativeDto create(@Valid @RequestBody SalesRepresentativeRequest request) {
        return salesRepresentativeService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public SalesRepresentativeDto update(@PathVariable UUID id, @Valid @RequestBody SalesRepresentativeRequest request) {
        return salesRepresentativeService.update(id, request);
    }

    @PostMapping("/{id}/active/{active}")
    @PreAuthorize("hasRole('OWNER')")
    public SalesRepresentativeDto setActive(@PathVariable UUID id, @PathVariable boolean active) {
        return salesRepresentativeService.setActive(id, active);
    }
}
