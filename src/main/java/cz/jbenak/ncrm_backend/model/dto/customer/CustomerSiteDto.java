package cz.jbenak.ncrm_backend.model.dto.customer;

import cz.jbenak.ncrm_backend.model.dto.AddressDto;

import java.util.UUID;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-11
 * DTO representing a physical site (branch, plant, warehouse) of a customer.
 */
public record CustomerSiteDto(
        UUID id,
        String name,
        AddressDto address,
        String note,
        boolean active
) {
}
