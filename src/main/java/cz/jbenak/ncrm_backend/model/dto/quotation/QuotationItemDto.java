package cz.jbenak.ncrm_backend.model.dto.quotation;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-22
 * DTO representing a single line of a price quotation.
 */
public record QuotationItemDto(
        UUID id,
        UUID itemId,
        String itemCode,
        String itemName,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal totalPrice
) {
}
