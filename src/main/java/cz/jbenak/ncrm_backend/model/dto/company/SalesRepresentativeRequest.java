package cz.jbenak.ncrm_backend.model.dto.company;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-13
 * Request DTO for creating or updating a sales representative by the administrator (owner).
 * Every sales representative must be linked to an existing user account.
 */
public record SalesRepresentativeRequest(
        @NotBlank @Size(max = 20) String code,
        @NotNull UUID userId,
        String phone,
        String businessEmail,
        @Size(max = 100) String region,
        String note,
        boolean active
) {
}
