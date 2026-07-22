package cz.jbenak.ncrm_backend.model.dto.store;

import cz.jbenak.ncrm_backend.model.entity.store.ItemEntity;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-15
 * Request DTO for creating or updating a catalogue item including its current price.
 */
public record ItemRequest(
        @NotBlank String code,
        @NotBlank String name,
        String description,
        @NotNull ItemEntity.ItemType itemType,
        UUID categoryId,
        String unit,
        boolean active,
        @NotNull @DecimalMin("0.00") BigDecimal price,
        @NotBlank String currency,
        @DecimalMin("0.00") BigDecimal purchasePriceNet,
        BigDecimal vatRate
) {
}
