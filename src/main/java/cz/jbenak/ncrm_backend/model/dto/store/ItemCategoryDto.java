package cz.jbenak.ncrm_backend.model.dto.store;

import java.util.List;
import java.util.UUID;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-11
 * DTO representing an item category node of the hierarchical category tree.
 */
public record ItemCategoryDto(
        UUID id,
        String code,
        String name,
        String description,
        String path,
        UUID parentId,
        int sortOrder,
        boolean active,
        List<ItemCategoryDto> children
) {
}
