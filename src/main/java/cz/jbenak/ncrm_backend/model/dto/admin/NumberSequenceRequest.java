package cz.jbenak.ncrm_backend.model.dto.admin;

import cz.jbenak.ncrm_backend.model.entity.NumberSequenceEntity;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-16
 * Request DTO for creating or updating a number sequence definition.
 * The prefix may contain a trailing separator (e.g. {@code OBJ-}); the year part and the
 * zero-padded counter are appended by the generator.
 */
public record NumberSequenceRequest(
        @NotNull NumberSequenceEntity.SequenceType type,
        @Size(max = 20) String prefix,
        boolean includeYear,
        @Min(1) @Max(12) int padding,
        @Min(1) long nextValue,
        boolean yearlyReset,
        @Size(max = 255) String description
) {
}
