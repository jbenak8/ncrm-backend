package cz.jbenak.ncrm_backend.model.dto.store;

import cz.jbenak.ncrm_backend.model.entity.store.ItemEntity;

import java.util.UUID;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-11
 * DTO representing an item (goods or service) offered by the company, including its current price.
 */
public record ItemDto(
        UUID id,
        String code,
        String name,
        String description,
        ItemEntity.ItemType itemType,
        UUID categoryId,
        String categoryName,
        String unit,
        boolean active,
        ItemPriceDto price
) {
}
