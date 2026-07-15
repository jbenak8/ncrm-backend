package cz.jbenak.ncrm_backend.search;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-14
 * Tests for parsing of raw {@code filter} query parameters into search criteria.
 */
class SearchCriterionTest {

    @Test
    void parsesSimpleExpression() {
        SearchCriterion criterion = SearchCriterion.parse("name:contains:teza");
        assertThat(criterion.field()).isEqualTo("name");
        assertThat(criterion.operator()).isEqualTo(SearchOperator.CONTAINS);
        assertThat(criterion.value()).isEqualTo("teza");
    }

    @Test
    void parsesValueContainingColons() {
        SearchCriterion criterion = SearchCriterion.parse("website:eq:https://www.teza-papir.cz/");
        assertThat(criterion.operator()).isEqualTo(SearchOperator.EQ);
        assertThat(criterion.value()).isEqualTo("https://www.teza-papir.cz/");
    }

    @Test
    void parsesBetweenExpression() {
        SearchCriterion criterion = SearchCriterion.parse("price.price:between:10,100");
        assertThat(criterion.field()).isEqualTo("price.price");
        assertThat(criterion.operator()).isEqualTo(SearchOperator.BETWEEN);
        assertThat(criterion.value()).isEqualTo("10,100");
    }

    @Test
    void rejectsMalformedExpression() {
        assertThatThrownBy(() -> SearchCriterion.parse("name:contains"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid filter expression");
    }

    @Test
    void rejectsUnknownOperator() {
        assertThatThrownBy(() -> SearchCriterion.parse("name:startsWith:abc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown search operator");
    }

    @Test
    void rejectsEmptyExpression() {
        assertThatThrownBy(() -> SearchCriterion.parse(" "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
