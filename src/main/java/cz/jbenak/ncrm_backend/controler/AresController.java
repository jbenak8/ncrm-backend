package cz.jbenak.ncrm_backend.controler;

import cz.jbenak.ncrm_backend.model.dto.ares.AresSubjectDto;
import cz.jbenak.ncrm_backend.services.AresService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-11
 * REST API for looking up economic subjects in the ARES registry, used to pre-fill
 * customer and company data when creating or editing them in the frontend.
 */
@RestController
@RequestMapping("/api/ares")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('OWNER', 'SALES_REPRESENTATIVE')")
public class AresController {

    private final AresService aresService;

    @GetMapping("/{registrationId}")
    public AresSubjectDto findByRegistrationId(@PathVariable String registrationId) {
        return aresService.findByRegistrationId(registrationId);
    }
}
