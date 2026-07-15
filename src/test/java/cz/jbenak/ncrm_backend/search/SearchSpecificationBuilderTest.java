package cz.jbenak.ncrm_backend.search;

import cz.jbenak.ncrm_backend.model.entity.customer.CustomerEntity;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
        assertThatThrownBy(() -> SearchSpecificationBuilder.build(List.of("password:eq:secret"), ALLOWED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Filtering by field 'password' is not supported");
    }

    @Test
    void rejectsMalformedFilterExpression() {
        assertThatThrownBy(() -> SearchSpecificationBuilder.build(List.of("name="), ALLOWED))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
