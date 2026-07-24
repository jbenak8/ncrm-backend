package cz.jbenak.ncrm_backend.model.dto.admin;

import cz.jbenak.ncrm_backend.model.entity.VATRate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-13
 * DTO representing a VAT rate of a country including its validity period.
 */
public record VatRateDto(
        UUID id,
        String countryIsoCode,
        String countryName,
        VATRate.VATType type,
        BigDecimal rate,
        LocalDateTime validFrom,
        LocalDateTime validTo
) {
}
