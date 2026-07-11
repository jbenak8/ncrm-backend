package cz.jbenak.ncrm_backend.model.dto.company;

import java.util.UUID;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-11
 * DTO representing a sales representative of the company.
 */
public record SalesRepresentativeDto(
        UUID id,
        String code,
        UUID userId,
        String firstName,
        String lastName,
        String phone,
        String businessEmail,
        String region,
        String note,
        boolean active
) {
}
