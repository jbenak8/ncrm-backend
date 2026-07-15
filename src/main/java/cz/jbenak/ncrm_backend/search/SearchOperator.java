package cz.jbenak.ncrm_backend.search;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-14
 * Operators supported by the generic search API. Each list endpoint accepts repeatable
 * {@code filter} query parameters in the form {@code field:operator:value}
 * (for {@link #BETWEEN} the value is {@code lower,upper}).
 */
public enum SearchOperator {

    /** String contains the given value (case-insensitive). */
    CONTAINS("contains"),
    /** String does not contain the given value (case-insensitive). */
    NOT_CONTAINS("notContains"),
    /** Value is equal to the given value. */
    EQ("eq"),
    /** Value is not equal to the given value. */
    NEQ("neq"),
    /** Value is less than the given value. */
    LT("lt"),
    /** Value is greater than the given value. */
    GT("gt"),
    /** Value is between the two given values (inclusive), separated by a comma. */
    BETWEEN("between");

    private final String token;

    SearchOperator(String token) {
        this.token = token;
    }

    public String token() {
        return token;
    }

    /**
     * Resolves an operator from its query token.
     *
     * @throws IllegalArgumentException when the token is not a known operator
     */
    public static SearchOperator fromToken(String token) {
        for (SearchOperator operator : values()) {
            if (operator.token.equalsIgnoreCase(token)) {
                return operator;
            }
        }
        throw new IllegalArgumentException("Unknown search operator '" + token + "'");
    }
}
