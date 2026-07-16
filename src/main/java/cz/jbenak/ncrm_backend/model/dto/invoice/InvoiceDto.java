package cz.jbenak.ncrm_backend.model.dto.invoice;

import cz.jbenak.ncrm_backend.model.entity.invoice.InvoiceEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-16
 * DTO representing an issued invoice including its snapshot lines and totals.
 */
public record InvoiceDto(
        UUID id,
        String invoiceNumber,
        UUID orderId,
        String orderNumber,
        UUID customerId,
        String customerName,
        InvoiceEntity.PaymentType paymentType,
        LocalDate issueDate,
        LocalDate taxDate,
        LocalDate dueDate,
        String variableSymbol,
        BigDecimal totalNet,
        BigDecimal totalVat,
        BigDecimal totalGross,
        String currency,
        String note,
        List<InvoiceItemDto> items
) {
}
