package cz.jbenak.ncrm_backend.model.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-13
 * DTO representing a country managed by the administrator. The ISO code is the natural identifier,
 * so the same record is used both for reading and for create/update requests.
 */
public record CountryDto(
        @NotBlank @Size(max = 3) String isoCode,
        @NotBlank String name,
        @NotBlank String nameEn,
        String nameLocal,
        boolean active,
        boolean euMember,
        boolean embargoed,
        @Size(max = 10) String vatShortCode,
        @NotBlank @Size(max = 4) String dialingCode
) {
}
