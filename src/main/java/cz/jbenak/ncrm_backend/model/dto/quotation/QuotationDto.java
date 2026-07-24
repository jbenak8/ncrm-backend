package cz.jbenak.ncrm_backend.model.dto.quotation;

import cz.jbenak.ncrm_backend.model.entity.quotation.QuotationEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-22
 * DTO representing a price quotation with its items, exposed to the React frontend.
 */
public record QuotationDto(
        UUID id,
        String quotationNumber,
        UUID customerId,
        String customerName,
        UUID companyId,
        String companyName,
        UUID contactPersonId,
        String contactPersonName,
        UUID salesRepresentativeId,
        String salesRepresentativeName,
        LocalDate quotationDate,
        LocalDate validUntil,
        QuotationEntity.QuotationStatus status,
        BigDecimal totalPrice,
        String currency,
        String note,
        UUID orderId,
        String orderNumber,
        List<QuotationItemDto> items
) {
}
