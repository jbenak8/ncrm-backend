package cz.jbenak.ncrm_backend.model.dto.company;

import cz.jbenak.ncrm_backend.model.dto.AddressDto;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-13
 * Request DTO for creating or updating an own company by the administrator (owner).
 * All text fields are trimmed on deserialization so that stray whitespace (e.g. " info@example.com")
 * does not fail bean validation such as {@code @Email}.
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

    public CompanyRequest {
        name = trim(name);
        nameSecondLine = trim(nameSecondLine);
        registrationId = trim(registrationId);
        vatId = trim(vatId);
        registrationNote = trim(registrationNote);
        registrationNoteEn = trim(registrationNoteEn);
        phone = trim(phone);
        email = trim(email);
        website = trim(website);
        bankAccount = trim(bankAccount);
        bankName = trim(bankName);
        iban = trim(iban);
        bic = trim(bic);
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }
}
