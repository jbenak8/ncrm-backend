package cz.jbenak.ncrm_backend.search;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-14
 * A single parsed search condition (column, operator and raw value) coming from
 * a {@code filter} query parameter in the form {@code field:operator:value}.
 */
public record SearchCriterion(String field, SearchOperator operator, String value) {

    /**
     * Parses a raw {@code filter} query parameter. The value part may contain further colons.
     *
     * @throws IllegalArgumentException when the expression is malformed
     */
    public static SearchCriterion parse(String expression) {
        if (expression == null || expression.isBlank()) {
            throw new IllegalArgumentException("Filter expression must not be empty");
        }
        String[] parts = expression.split(":", 3);
        if (parts.length < 3 || parts[0].isBlank() || parts[1].isBlank()) {
            throw new IllegalArgumentException(
                    "Invalid filter expression '" + expression + "'. Expected format field:operator:value");
        }
        return new SearchCriterion(parts[0].trim(), SearchOperator.fromToken(parts[1].trim()), parts[2].trim());
    }
}
