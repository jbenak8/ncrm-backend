package cz.jbenak.ncrm_backend.model.dto.ares;

import cz.jbenak.ncrm_backend.model.dto.AddressDto;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-11
 * DTO representing an economic subject looked up in the Czech ARES registry (or a similar EU registry).
 * Used to pre-fill customer and company data on the frontend.
 */
public record AresSubjectDto(
        String registrationId,
        String vatId,
        String name,
        String legalForm,
        AddressDto address
) {
}
