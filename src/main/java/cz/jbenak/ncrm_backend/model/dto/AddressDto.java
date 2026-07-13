package cz.jbenak.ncrm_backend.model.dto;

import java.util.UUID;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-11
 * DTO representing a postal address exposed to the React frontend.
 */
public record AddressDto(
        UUID id,
        String street,
        String houseNumber,
        String streetNumber,
        String city,
        String zipCode,
        String countryIsoCode,
        String countryName
) {
}
