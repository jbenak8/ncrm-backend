package cz.jbenak.ncrm_backend.controler;

import cz.jbenak.ncrm_backend.model.dto.admin.VatRateDto;
import cz.jbenak.ncrm_backend.model.dto.admin.VatRateRequest;
import cz.jbenak.ncrm_backend.services.VatRateService;
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
 * REST API for VAT rate administration. Reading is available to every authenticated user
 * (rates are needed when pricing orders), while all modifications are restricted to the owner.
 */
@RestController
@RequestMapping("/api/vat-rates")
@RequiredArgsConstructor
public class VatRateController {

    private final VatRateService vatRateService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<VatRateDto> findAll(@RequestParam(required = false) String countryIsoCode) {
        return countryIsoCode == null ? vatRateService.findAll() : vatRateService.findByCountry(countryIsoCode);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public VatRateDto findById(@PathVariable UUID id) {
        return vatRateService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('OWNER')")
    public VatRateDto create(@Valid @RequestBody VatRateRequest request) {
        return vatRateService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public VatRateDto update(@PathVariable UUID id, @Valid @RequestBody VatRateRequest request) {
        return vatRateService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('OWNER')")
    public void delete(@PathVariable UUID id) {
        vatRateService.delete(id);
    }
}
