package cz.jbenak.ncrm_backend.model.dto.store;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-15
 * Result of an item delete request. When the item is referenced by at least one order,
 * it is only deactivated instead of being removed (historical orders must stay intact).
 */
public record ItemDeleteResult(
        boolean deleted,
        boolean deactivated
) {
}
