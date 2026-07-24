package cz.jbenak.ncrm_backend.security;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-14
 * Application password policy: at least 8 characters long, containing at least one upper-case letter,
 * one lower-case letter, one digit and one special (non-alphanumeric) character.
 */
public final class PasswordPolicy {

    public static final String DESCRIPTION =
            "Password must be at least 8 characters long and contain at least one upper-case letter, "
                    + "one lower-case letter, one digit and one special character";

    private PasswordPolicy() {
    }

    /**
     * Checks whether the given password satisfies the policy.
     */
    public static boolean isValid(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        return password.chars().anyMatch(Character::isUpperCase)
                && password.chars().anyMatch(Character::isLowerCase)
                && password.chars().anyMatch(Character::isDigit)
                && password.chars().anyMatch(c -> !Character.isLetterOrDigit(c));
    }

    /**
     * Validates the given password against the policy.
     *
     * @throws IllegalArgumentException when the password does not satisfy the policy
     */
    public static void validate(String password) {
        if (!isValid(password)) {
            throw new IllegalArgumentException(DESCRIPTION);
        }
    }
}
