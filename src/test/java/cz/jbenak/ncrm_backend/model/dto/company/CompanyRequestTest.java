package cz.jbenak.ncrm_backend.model.dto.company;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-14
 * Reproduces the reported 400 Bad Request when creating a company with an email containing
 * a leading space (" info@teza-papir.cz") and verifies that trimming makes the request valid.
 */
class CompanyRequestTest {

    @Test
    void trimsTextFieldsSoEmailWithLeadingSpacePassesValidation() {
        CompanyRequest request = new CompanyRequest(
                "TEZA MV s.r.o.", "", "26162644", "CZ26162644", null,
                "Společnost zapsaná v obchodním rejstříku.", "Company registered at Commercial register.",
                "603 440 412", " info@teza-papir.cz", "https://www.teza-papir.cz/",
                "2171560183/0800", "Česká spořitelna", "CZ3208000000002171560183", "GIBACZPX",
                true, true);

        assertThat(request.email()).isEqualTo("info@teza-papir.cz");

        try (var factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            Set<ConstraintViolation<CompanyRequest>> violations = validator.validate(request);
            assertThat(violations).isEmpty();
        }
    }
}
