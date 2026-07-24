package cz.jbenak.ncrm_backend.model.dto.customer;

import cz.jbenak.ncrm_backend.model.dto.AddressDto;

import java.util.List;
import java.util.UUID;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-11
 * DTO representing a customer with its contact persons and sites, exposed to the React frontend.
 */
public record CustomerDto(
        UUID id,
        String designation,
        String name,
        String registrationId,
        String vatId,
        String email,
        String phone,
        AddressDto headquartersAddress,
        UUID salesRepresentativeId,
        String salesRepresentativeName,
        boolean active,
        String note,
        List<ContactPersonDto> contactPersons,
        List<CustomerSiteDto> sites
) {
}
