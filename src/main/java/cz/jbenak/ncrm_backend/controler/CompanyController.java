package cz.jbenak.ncrm_backend.controler;

import cz.jbenak.ncrm_backend.model.dto.company.CompanyDto;
import cz.jbenak.ncrm_backend.model.dto.company.CompanyLogoDto;
import cz.jbenak.ncrm_backend.model.dto.company.CompanyRequest;
import cz.jbenak.ncrm_backend.security.CustomerScope;
import cz.jbenak.ncrm_backend.services.CompanyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-13
 * REST API for administration of own companies. All operations are restricted to the owner.
 */
@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;
    private final CustomerScope customerScope;

    // Sales representatives may list the companies as well (scoped to their assignment)
    // so that the frontend can filter the dashboard data by the active company. Customers
    // may list strictly the companies assigned to their account (needed to place orders).
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'SALES_REPRESENTATIVE', 'CUSTOMER')")
    public List<CompanyDto> findAll(Authentication authentication) {
        if (customerScope.isCustomer(authentication)) {
            return companyService.findAssignedTo(authentication.getName());
        }
        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        return companyService.findAllForUser(authentication.getName(), admin);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public CompanyDto findById(@PathVariable UUID id) {
        return companyService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public CompanyDto create(@Valid @RequestBody CompanyRequest request) {
        return companyService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public CompanyDto update(@PathVariable UUID id, @Valid @RequestBody CompanyRequest request) {
        return companyService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public void delete(@PathVariable UUID id, Authentication authentication) {
        companyService.delete(id, authentication.getName());
    }

    @PostMapping("/{id}/default")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public CompanyDto setDefault(@PathVariable UUID id) {
        return companyService.setDefault(id);
    }

    @PutMapping(value = "/{id}/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public CompanyDto uploadLogo(@PathVariable UUID id, @RequestParam("file") MultipartFile file) {
        return companyService.uploadLogo(id, file);
    }

    @GetMapping("/{id}/logo")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'SALES_REPRESENTATIVE', 'CUSTOMER')")
    public ResponseEntity<byte[]> getLogo(@PathVariable UUID id) {
        CompanyLogoDto logo = companyService.getLogo(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(logo.contentType()))
                .body(logo.content());
    }

    @DeleteMapping("/{id}/logo")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public void deleteLogo(@PathVariable UUID id) {
        companyService.deleteLogo(id);
    }
}
