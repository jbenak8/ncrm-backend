package cz.jbenak.ncrm_backend.model.dto.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-14
 * Request DTO for changing the password of the currently authenticated user.
 * The new password must satisfy the password policy (see PasswordPolicy).
 */
public record ChangePasswordRequest(
        @NotBlank String currentPassword,
        @NotBlank @Size(min = 8, max = 100) String newPassword
) {
}
