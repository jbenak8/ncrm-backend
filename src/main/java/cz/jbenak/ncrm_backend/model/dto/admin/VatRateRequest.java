package cz.jbenak.ncrm_backend.model.dto.admin;

import cz.jbenak.ncrm_backend.model.entity.VATRate;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-13
 * Request DTO for creating or updating a VAT rate of a country.
 */
public record VatRateRequest(
        @NotBlank String countryIsoCode,
        @NotNull VATRate.VATType type,
        @NotNull @DecimalMin("0.00") BigDecimal rate,
        @NotNull LocalDateTime validFrom,
        LocalDateTime validTo
) {
}
