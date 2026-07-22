package cz.jbenak.ncrm_backend.model.dto.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-11
 * Request DTO for creating or updating an order. Unit prices are resolved server-side
 * from the current item price at the time of ordering.
 */
public record OrderRequest(
        @NotNull UUID customerId,
        // Own company issuing the order; when null, the default company is used.
        UUID companyId,
        UUID contactPersonId,
        @NotNull UUID salesRepresentativeId,
        @NotNull LocalDate orderDate,
        String currency,
        String note,
        @NotEmpty @Valid List<OrderItemRequest> items
) {

    /**
     * Single order line of the request.
     */
    public record OrderItemRequest(
            @NotNull UUID itemId,
            @NotNull @Positive BigDecimal quantity
    ) {
    }
}
