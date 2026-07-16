package cz.jbenak.ncrm_backend.search;

import cz.jbenak.ncrm_backend.model.entity.customer.CustomerEntity;
import cz.jbenak.ncrm_backend.model.entity.order.OrderEntity;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-14
 * Tests for the generic search specification builder (whitelisting and input validation).
 */
class SearchSpecificationBuilderTest {

    private static final Set<String> ALLOWED = Set.of("name", "active", "registrationId");

    @Test
    void buildsSpecificationForAllowedFields() {
        Specification<CustomerEntity> specification = SearchSpecificationBuilder.build(
                List.of("name:contains:teza", "active:eq:true"), ALLOWED);
        assertThat(specification).isNotNull();
    }

    @Test
    void buildsEmptySpecificationWhenNoFiltersGiven() {
        assertThat(SearchSpecificationBuilder.<CustomerEntity>build(null, ALLOWED)).isNotNull();
        assertThat(SearchSpecificationBuilder.<CustomerEntity>build(List.of(), ALLOWED)).isNotNull();
    }

    @Test
    void rejectsFieldOutsideWhitelist() {
        List<String> filters = List.of("password:eq:secret");
        assertThatThrownBy(() -> SearchSpecificationBuilder.build(filters, ALLOWED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Filtering by field 'password' is not supported");
    }

    @Test
    void rejectsMalformedFilterExpression() {
        List<String> filters = List.of("name=");
        assertThatThrownBy(() -> SearchSpecificationBuilder.build(filters, ALLOWED))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --- execution of the built predicates against a mocked criteria API ---

    @SuppressWarnings("unchecked")
    private final Root<Object> root = mock(Root.class);
    @SuppressWarnings("unchecked")
    private final CriteriaQuery<Object> query = mock(CriteriaQuery.class);
    private final CriteriaBuilder cb = mock(CriteriaBuilder.class);
    @SuppressWarnings("rawtypes")
    private final Path path = mock(Path.class);

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUpCriteriaMocks() {
        lenient().when(root.get("value")).thenReturn(path);
        lenient().when(path.get("nested")).thenReturn(path);
        lenient().when(cb.and(any(Predicate[].class))).thenReturn(mock(Predicate.class));
        lenient().when(cb.lower(any(Expression.class))).thenReturn(mock(Expression.class));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Predicate apply(String filter, Class<?> type) {
        lenient().when(path.getJavaType()).thenReturn((Class) type);
        Specification<Object> specification =
                SearchSpecificationBuilder.build(List.of(filter), Set.of("value", "value.nested"));
        return specification.toPredicate(root, query, cb);
    }

    @Test
    void eqConvertsValueAccordingToAttributeType() {
        apply("value:eq:true", Boolean.class);
        verify(cb).equal(path, Boolean.TRUE);

        apply("value:eq:42", Integer.class);
        verify(cb).equal(path, 42);

        apply("value:eq:42", Long.class);
        verify(cb).equal(path, 42L);

        apply("value:eq:1.50", BigDecimal.class);
        verify(cb).equal(path, new BigDecimal("1.50"));

        apply("value:eq:1.5", Double.class);
        verify(cb).equal(path, 1.5d);

        UUID uuid = UUID.randomUUID();
        apply("value:eq:" + uuid, UUID.class);
        verify(cb).equal(path, uuid);

        apply("value:eq:2026-07-16", LocalDate.class);
        verify(cb).equal(path, LocalDate.of(2026, Month.JULY, 16));

        apply("value:eq:2026-07-16T10:00:00", LocalDateTime.class);
        verify(cb).equal(path, LocalDateTime.of(2026, Month.JULY, 16, 10, 0));

        apply("value:eq:2026-07-16T10:00:00+02:00", OffsetDateTime.class);
        verify(cb).equal(path, OffsetDateTime.parse("2026-07-16T10:00:00+02:00"));
    }

    @Test
    void eqConvertsEnumValueCaseInsensitively() {
        apply("value:eq:completed", OrderEntity.OrderStatus.class);
        verify(cb).equal(path, OrderEntity.OrderStatus.COMPLETED);
    }

    @Test
    @SuppressWarnings("unchecked")
    void containsBuildsLowercasedEscapedLikePattern() {
        apply("value:contains:A%b_c\\d", String.class);
        verify(cb).like(any(Expression.class), eq("%a\\%b\\_c\\\\d%"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void notContainsBuildsNotLikePredicate() {
        apply("value:notContains:abc", String.class);
        verify(cb).notLike(any(Expression.class), eq("%abc%"));
    }

    @Test
    void containsIsRejectedForNonTextFields() {
        assertThatThrownBy(() -> apply("value:contains:1", Integer.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("only supported for text fields");
    }

    @Test
    @SuppressWarnings("unchecked")
    void neqLtAndGtUseConvertedValues() {
        apply("value:neq:5", Integer.class);
        verify(cb).notEqual(path, 5);

        apply("value:lt:10", Integer.class);
        verify(cb).lessThan(any(Expression.class), eq(10));

        apply("value:gt:2026-01-01", LocalDate.class);
        verify(cb).greaterThan(any(Expression.class), eq(LocalDate.of(2026, Month.JANUARY, 1)));
    }

    @Test
    @SuppressWarnings("unchecked")
    void betweenUsesBothTrimmedBounds() {
        apply("value:between:1, 10", Integer.class);
        verify(cb).between(any(Expression.class), eq(1), eq(10));
    }

    @Test
    void betweenRequiresTwoBounds() {
        assertThatThrownBy(() -> apply("value:between:1", Integer.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("two comma-separated values");
        assertThatThrownBy(() -> apply("value:between:1,", Integer.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("two comma-separated values");
    }

    @Test
    void rejectsValueNotConvertibleToAttributeType() {
        assertThatThrownBy(() -> apply("value:eq:abc", Integer.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid value 'abc'");
        assertThatThrownBy(() -> apply("value:eq:not-a-date", LocalDate.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid value 'not-a-date'");
    }

    @Test
    void rejectsAttributeTypeWithoutConverter() {
        assertThatThrownBy(() -> apply("value:eq:x", byte[].class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("is not supported");
    }

    @Test
    void resolvesNestedAttributePaths() {
        assertThatCode(() -> apply("value.nested:eq:abc", String.class)).doesNotThrowAnyException();
        verify(cb).equal(path, "abc");
    }
}
