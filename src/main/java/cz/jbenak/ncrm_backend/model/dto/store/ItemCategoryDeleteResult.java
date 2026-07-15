package cz.jbenak.ncrm_backend.model.dto.store;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-15
 * Result of a category delete request. When the category (or its subtree) still contains items
 * and the request is not forced, nothing is deleted and {@code requiresConfirmation} is set so
 * the frontend can warn the user that the assigned items will be removed (or deactivated when
 * they are referenced by orders).
 */
public record ItemCategoryDeleteResult(
        boolean deleted,
        boolean requiresConfirmation,
        long affectedItems,
        long deletedItems,
        long deactivatedItems
) {
}
