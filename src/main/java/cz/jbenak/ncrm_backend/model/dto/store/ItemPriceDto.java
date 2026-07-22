package cz.jbenak.ncrm_backend.model.dto.store;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-11
 * DTO representing the price of an item including currency, VAT rate and validity period.
 */
public record ItemPriceDto(
        UUID id,
        BigDecimal price,
        String currency,
        BigDecimal purchasePriceNet,
        BigDecimal vatRate,
        LocalDateTime validFrom,
        LocalDateTime validTo
) {
}
