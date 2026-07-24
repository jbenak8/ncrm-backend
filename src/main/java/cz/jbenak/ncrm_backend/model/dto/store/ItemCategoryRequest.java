package cz.jbenak.ncrm_backend.model.dto.store;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-15
 * Request DTO for creating or updating an item category of the hierarchical category tree.
 */
public record ItemCategoryRequest(
        @NotBlank String code,
        @NotBlank String name,
        String description,
        UUID parentId,
        int sortOrder,
        boolean active
) {
}
