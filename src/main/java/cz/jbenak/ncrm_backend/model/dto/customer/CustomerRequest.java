package cz.jbenak.ncrm_backend.model.dto.customer;

import cz.jbenak.ncrm_backend.model.dto.AddressDto;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-11
 * Request DTO for creating or updating a customer. The address data can be pre-filled
 * from the ARES registry (or similar EU registries) on the frontend.
 */
public record CustomerRequest(
        String designation,
        @NotBlank String name,
        @NotBlank String registrationId,
        String vatId,
        @Email String email,
        String phone,
        AddressDto headquartersAddress,
        UUID salesRepresentativeId,
        boolean active,
        String note
) {
}
