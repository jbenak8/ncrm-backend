package cz.jbenak.ncrm_backend.model.dto.invoice;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-16
 * DTO representing a single line of an issued invoice (a snapshot of the ordered item
 * including the applied VAT rate and computed VAT amounts).
 */
public record InvoiceItemDto(
        UUID id,
        String itemCode,
        String itemName,
        BigDecimal quantity,
        String unit,
        BigDecimal unitPrice,
        BigDecimal vatRate,
        BigDecimal totalNet,
        BigDecimal totalVat,
        BigDecimal totalGross
) {
}
