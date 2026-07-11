package cz.jbenak.ncrm_backend.model.dto.security;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-11
 * DTO representing an application user account. The password hash is intentionally never exposed.
 */
public record UserDto(
        UUID id,
        String username,
        String email,
        String firstName,
        String lastName,
        boolean enabled,
        boolean locked,
        LocalDateTime lastLoginAt,
        Set<String> roles
) {
}
