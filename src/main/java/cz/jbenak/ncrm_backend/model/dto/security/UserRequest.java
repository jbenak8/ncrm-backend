package cz.jbenak.ncrm_backend.model.dto.security;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.UUID;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-13
 * Request DTO for creating or updating a user account by the administrator (owner).
 * The password is mandatory on creation and optional on update (blank means "keep the current password").
 */
public record UserRequest(
        @NotBlank @Size(max = 100) String username,
        @NotBlank @Email String email,
        @Size(min = 8, max = 100) String password,
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        boolean enabled,
        boolean locked,
        boolean mustChangePassword,
        boolean sendCredentials,
        Set<String> roles,
        Set<UUID> companyIds
) {
}
