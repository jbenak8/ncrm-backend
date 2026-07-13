package cz.jbenak.ncrm_backend.model.dto.company;

import cz.jbenak.ncrm_backend.model.dto.AddressDto;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-13
 * Request DTO for creating or updating an own company by the administrator (owner).
 */
public record CompanyRequest(
        @NotBlank String name,
        String nameSecondLine,
        @NotBlank String registrationId,
        String vatId,
        AddressDto address,
        String registrationNote,
        String registrationNoteEn,
        String phone,
        @Email String email,
        String website,
        String bankAccount,
        String bankName,
        String iban,
        String bic,
        boolean active,
        boolean defaultCompany
) {
}
