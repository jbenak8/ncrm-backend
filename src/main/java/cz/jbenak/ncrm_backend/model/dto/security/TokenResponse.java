package cz.jbenak.ncrm_backend.model.dto.security;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-23
 * Response DTO returned after a successful login in the "db-auth" profile. The shape follows
 * the OAuth2 token response so the React frontend can treat both Keycloak and database-issued
 * tokens uniformly; "mustChangePassword" tells the frontend to force a password change dialog.
 */
public record TokenResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        boolean mustChangePassword
) {
}
