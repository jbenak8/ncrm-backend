package cz.jbenak.ncrm_backend.model.dto.admin;

import cz.jbenak.ncrm_backend.model.entity.NumberSequenceEntity;

import java.util.UUID;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-16
 * DTO representing a number sequence definition for order or invoice numbers,
 * including a preview of the next number that will be generated.
 */
public record NumberSequenceDto(
        UUID id,
        NumberSequenceEntity.SequenceType type,
        String prefix,
        boolean includeYear,
        int padding,
        long nextValue,
        boolean yearlyReset,
        Integer lastResetYear,
        String description,
        String nextNumberPreview
) {
}
