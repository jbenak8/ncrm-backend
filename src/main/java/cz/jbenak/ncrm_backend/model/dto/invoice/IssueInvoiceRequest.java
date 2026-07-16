package cz.jbenak.ncrm_backend.model.dto.invoice;

import cz.jbenak.ncrm_backend.model.entity.invoice.InvoiceEntity;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-16
 * Request DTO for issuing an invoice for a completed order. The payment type determines
 * the layout of the printed invoice: a bank transfer invoice carries the bank account,
 * variable symbol and a payment QR code, a cash invoice does not. When {@code dueDays}
 * is {@code null}, the default of 14 days is used.
 */
public record IssueInvoiceRequest(
        @NotNull UUID orderId,
        @NotNull InvoiceEntity.PaymentType paymentType,
        @Min(0) @Max(365) Integer dueDays,
        @Size(max = 255) String note
) {
}
