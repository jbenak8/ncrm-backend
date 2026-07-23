package cz.jbenak.ncrm_backend.model.dto.security;

import jakarta.validation.constraints.NotBlank;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-23
 * Request DTO for logging in with the credentials stored in the application database ("db-auth" profile).
 */
public record LoginRequest(
        @NotBlank String username,
        @NotBlank String password
) {
}
