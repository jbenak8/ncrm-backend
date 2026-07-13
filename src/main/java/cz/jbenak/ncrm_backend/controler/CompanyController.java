package cz.jbenak.ncrm_backend.controler;

import cz.jbenak.ncrm_backend.model.dto.company.CompanyDto;
import cz.jbenak.ncrm_backend.model.dto.company.CompanyLogoDto;
import cz.jbenak.ncrm_backend.model.dto.company.CompanyRequest;
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

    @GetMapping
    @PreAuthorize("hasRole('OWNER')")
    public List<CompanyDto> findAll() {
        return companyService.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public CompanyDto findById(@PathVariable UUID id) {
        return companyService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('OWNER')")
    public CompanyDto create(@Valid @RequestBody CompanyRequest request) {
        return companyService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public CompanyDto update(@PathVariable UUID id, @Valid @RequestBody CompanyRequest request) {
        return companyService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('OWNER')")
    public void delete(@PathVariable UUID id, Authentication authentication) {
        companyService.delete(id, authentication.getName());
    }

    @PostMapping("/{id}/default")
    @PreAuthorize("hasRole('OWNER')")
    public CompanyDto setDefault(@PathVariable UUID id) {
        return companyService.setDefault(id);
    }

    @PutMapping(value = "/{id}/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('OWNER')")
    public CompanyDto uploadLogo(@PathVariable UUID id, @RequestParam("file") MultipartFile file) {
        return companyService.uploadLogo(id, file);
    }

    @GetMapping("/{id}/logo")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<byte[]> getLogo(@PathVariable UUID id) {
        CompanyLogoDto logo = companyService.getLogo(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(logo.contentType()))
                .body(logo.content());
    }

    @DeleteMapping("/{id}/logo")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('OWNER')")
    public void deleteLogo(@PathVariable UUID id) {
        companyService.deleteLogo(id);
    }
}
