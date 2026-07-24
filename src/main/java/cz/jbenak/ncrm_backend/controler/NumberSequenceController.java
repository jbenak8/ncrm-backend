package cz.jbenak.ncrm_backend.controler;

import cz.jbenak.ncrm_backend.model.dto.admin.NumberSequenceDto;
import cz.jbenak.ncrm_backend.model.dto.admin.NumberSequenceRequest;
import cz.jbenak.ncrm_backend.model.entity.NumberSequenceEntity;
import cz.jbenak.ncrm_backend.services.NumberSequenceService;
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
 * @since 2026-07-16
 * REST API for administration of number sequences used to generate order and invoice numbers.
 * Reading is available to every authenticated user, while all modifications are restricted
 * to administrators and the owner.
 */
@RestController
@RequestMapping("/api/number-sequences")
@RequiredArgsConstructor
public class NumberSequenceController {

    private final NumberSequenceService numberSequenceService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<NumberSequenceDto> findAll() {
        return numberSequenceService.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public NumberSequenceDto findById(@PathVariable UUID id) {
        return numberSequenceService.findById(id);
    }

    @GetMapping("/type/{type}")
    @PreAuthorize("isAuthenticated()")
    public NumberSequenceDto findByType(@PathVariable NumberSequenceEntity.SequenceType type) {
        return numberSequenceService.findByType(type);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public NumberSequenceDto create(@Valid @RequestBody NumberSequenceRequest request) {
        return numberSequenceService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public NumberSequenceDto update(@PathVariable UUID id, @Valid @RequestBody NumberSequenceRequest request) {
        return numberSequenceService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public void delete(@PathVariable UUID id) {
        numberSequenceService.delete(id);
    }
}
