package cz.jbenak.ncrm_backend.model.dto.customer;

import java.util.UUID;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-11
 * DTO representing a contact person of a customer.
 */
public record ContactPersonDto(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String phone,
        String position,
        String note,
        boolean active
) {
}
