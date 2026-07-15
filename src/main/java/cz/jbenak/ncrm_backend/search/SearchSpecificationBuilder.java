package cz.jbenak.ncrm_backend.search;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-14
 * Builds a JPA {@link Specification} from raw {@code filter} query parameters
 * ({@code field:operator:value}). Only whitelisted fields of the entity may be queried;
 * values are converted according to the Java type of the targeted attribute.
 * All criteria are combined with a logical AND.
 */
public final class SearchSpecificationBuilder {

    private SearchSpecificationBuilder() {
    }

    /**
     * Builds a specification from the given raw filter expressions.
     *
     * @param filters       raw expressions in the form {@code field:operator:value}; may be null or empty
     * @param allowedFields whitelist of entity attribute paths that may be filtered
     * @throws IllegalArgumentException when an expression is malformed, targets a non-whitelisted
     *                                  field or uses an operator unsupported for the attribute type
     */
    public static <T> Specification<T> build(List<String> filters, Set<String> allowedFields) {
        List<SearchCriterion> criteria = filters == null
                ? List.of()
                : filters.stream().map(SearchCriterion::parse).toList();
        criteria.forEach(criterion -> {
            if (!allowedFields.contains(criterion.field())) {
                throw new IllegalArgumentException("Filtering by field '" + criterion.field() + "' is not supported."
                        + " Supported fields: " + String.join(", ", allowedFields.stream().sorted().toList()));
            }
        });
        return (root, query, cb) -> cb.and(criteria.stream()
                .map(criterion -> toPredicate(criterion, root, cb))
                .toArray(Predicate[]::new));
    }

    private static <T> Predicate toPredicate(SearchCriterion criterion, Root<T> root, CriteriaBuilder cb) {
        Path<?> path = resolvePath(root, criterion.field());
        Class<?> type = path.getJavaType();
        return switch (criterion.operator()) {
            case CONTAINS -> cb.like(cb.lower(asString(path, type)), containsPattern(criterion.value()));
            case NOT_CONTAINS -> cb.notLike(cb.lower(asString(path, type)), containsPattern(criterion.value()));
            case EQ -> cb.equal(path, convert(criterion, type, criterion.value()));
            case NEQ -> cb.notEqual(path, convert(criterion, type, criterion.value()));
            case LT -> cb.lessThan(asComparable(path), comparable(criterion, type, criterion.value()));
            case GT -> cb.greaterThan(asComparable(path), comparable(criterion, type, criterion.value()));
            case BETWEEN -> {
                String[] bounds = criterion.value().split(",", 2);
                if (bounds.length != 2 || bounds[0].isBlank() || bounds[1].isBlank()) {
                    throw new IllegalArgumentException("Operator 'between' for field '" + criterion.field()
                            + "' requires two comma-separated values (lower,upper)");
                }
                yield cb.between(asComparable(path),
                        comparable(criterion, type, bounds[0].trim()),
                        comparable(criterion, type, bounds[1].trim()));
            }
        };
    }

    private static <T> Path<?> resolvePath(Root<T> root, String field) {
        Path<?> path = root;
        for (String part : field.split("\\.")) {
            path = path.get(part);
        }
        return path;
    }

    private static Expression<String> asString(Path<?> path, Class<?> type) {
        if (!String.class.equals(type)) {
            throw new IllegalArgumentException("Operators 'contains'/'notContains' are only supported for text fields");
        }
        @SuppressWarnings("unchecked")
        Expression<String> expression = (Expression<String>) path;
        return expression;
    }

    private static String containsPattern(String value) {
        return "%" + value.toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_") + "%";
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Expression<Comparable> asComparable(Path<?> path) {
        return (Expression<Comparable>) path;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Comparable comparable(SearchCriterion criterion, Class<?> type, String rawValue) {
        Object converted = convert(criterion, type, rawValue);
        if (!(converted instanceof Comparable comparable)) {
            throw new IllegalArgumentException("Operators 'lt'/'gt'/'between' are not supported for field '"
                    + criterion.field() + "'");
        }
        return comparable;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object convert(SearchCriterion criterion, Class<?> type, String rawValue) {
        try {
            if (String.class.equals(type)) {
                return rawValue;
            }
            if (Boolean.class.equals(type) || boolean.class.equals(type)) {
                return Boolean.parseBoolean(rawValue);
            }
            if (Integer.class.equals(type) || int.class.equals(type)) {
                return Integer.valueOf(rawValue);
            }
            if (Long.class.equals(type) || long.class.equals(type)) {
                return Long.valueOf(rawValue);
            }
            if (BigDecimal.class.equals(type)) {
                return new BigDecimal(rawValue);
            }
            if (Double.class.equals(type) || double.class.equals(type)) {
                return Double.valueOf(rawValue);
            }
            if (UUID.class.equals(type)) {
                return UUID.fromString(rawValue);
            }
            if (LocalDate.class.equals(type)) {
                return LocalDate.parse(rawValue);
            }
            if (LocalDateTime.class.equals(type)) {
                return LocalDateTime.parse(rawValue);
            }
            if (OffsetDateTime.class.equals(type)) {
                return OffsetDateTime.parse(rawValue);
            }
            if (type.isEnum()) {
                return Enum.valueOf((Class<Enum>) type, rawValue.toUpperCase(Locale.ROOT));
            }
        } catch (IllegalArgumentException | java.time.format.DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid value '" + rawValue + "' for field '"
                    + criterion.field() + "' of type " + type.getSimpleName());
        }
        throw new IllegalArgumentException("Filtering by field '" + criterion.field() + "' of type "
                + type.getSimpleName() + " is not supported");
    }
}
