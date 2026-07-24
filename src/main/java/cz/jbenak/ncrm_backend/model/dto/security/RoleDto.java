package cz.jbenak.ncrm_backend.model.dto.security;

import java.util.UUID;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-14
 * DTO representing a security role that can be assigned to a user account.
 */
public record RoleDto(
        UUID id,
        String name,
        String description
) {
}
