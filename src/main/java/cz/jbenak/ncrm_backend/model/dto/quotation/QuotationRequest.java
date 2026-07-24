package cz.jbenak.ncrm_backend.model.dto.quotation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-22
 * Request DTO for creating or updating a price quotation. Unit prices default to the current
 * item price, but each line may override the unit price or the line total manually.
 */
public record QuotationRequest(
        @NotNull UUID customerId,
        // Own company issuing the quotation; when null, the default company is used.
        UUID companyId,
        UUID contactPersonId,
        @NotNull UUID salesRepresentativeId,
        @NotNull LocalDate quotationDate,
        LocalDate validUntil,
        String currency,
        String note,
        @NotEmpty @Valid List<QuotationItemRequest> items
) {

    /**
     * Single quotation line of the request. When {@code unitPrice} is given, it overrides the
     * snapshot of the current item price; when {@code totalPrice} is given, it overrides the
     * computed line total (quantity × unit price).
     */
    public record QuotationItemRequest(
            @NotNull UUID itemId,
            @NotNull @Positive BigDecimal quantity,
            @PositiveOrZero BigDecimal unitPrice,
            @PositiveOrZero BigDecimal totalPrice
    ) {
    }
}
