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
        boolean upper = false;
        boolean lower = false;
        boolean digit = false;
        boolean special = false;
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) {
                upper = true;
            } else if (Character.isLowerCase(c)) {
                lower = true;
            } else if (Character.isDigit(c)) {
                digit = true;
            } else {
                special = true;
            }
        }
        return upper && lower && digit && special;
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
