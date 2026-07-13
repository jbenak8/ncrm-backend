package cz.jbenak.ncrm_backend.model.dto.company;

import cz.jbenak.ncrm_backend.model.dto.AddressDto;

import java.util.UUID;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-13
 * DTO representing an own company managed by the administrator (owner).
 */
public record CompanyDto(
        UUID id,
        String name,
        String nameSecondLine,
        String registrationId,
        String vatId,
        AddressDto address,
        String registrationNote,
        String registrationNoteEn,
        String phone,
        String email,
        String website,
        String bankAccount,
        String bankName,
        String iban,
        String bic,
        boolean active,
        boolean defaultCompany,
        boolean hasLogo
) {
}
