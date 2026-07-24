package cz.jbenak.ncrm_backend.model.dto.order;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-11
 * DTO representing a single line of an order.
 */
public record OrderItemDto(
        UUID id,
        UUID itemId,
        String itemCode,
        String itemName,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal totalPrice
) {
}
