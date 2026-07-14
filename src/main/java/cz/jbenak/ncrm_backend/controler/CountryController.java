package cz.jbenak.ncrm_backend.controler;

import cz.jbenak.ncrm_backend.model.dto.admin.CountryDto;
import cz.jbenak.ncrm_backend.services.CountryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-13
 * REST API for country administration. Reading is available to every authenticated user
 * (countries are used in address forms), while all modifications are restricted to the owner.
 */
@RestController
@RequestMapping("/api/countries")
@RequiredArgsConstructor
public class CountryController {

    private final CountryService countryService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<CountryDto> findAll(@RequestParam(defaultValue = "false") boolean activeOnly) {
        return countryService.findAll(activeOnly);
    }

    @GetMapping("/{isoCode}")
    @PreAuthorize("isAuthenticated()")
    public CountryDto findByIsoCode(@PathVariable String isoCode) {
        return countryService.findByIsoCode(isoCode);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public CountryDto create(@Valid @RequestBody CountryDto request) {
        return countryService.create(request);
    }

    @PutMapping("/{isoCode}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public CountryDto update(@PathVariable String isoCode, @Valid @RequestBody CountryDto request) {
        return countryService.update(isoCode, request);
    }

    @PostMapping("/{isoCode}/active/{active}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public CountryDto setActive(@PathVariable String isoCode, @PathVariable boolean active) {
        return countryService.setActive(isoCode, active);
    }
}
