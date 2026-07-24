package cz.jbenak.ncrm_backend.model.dto.customer;

import cz.jbenak.ncrm_backend.model.entity.customer.MeetingEntity;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-11
 * DTO representing a meeting between a sales representative and a customer, including the meeting minutes (outcome).
 */
public record MeetingDto(
        UUID id,
        UUID customerId,
        String customerName,
        UUID companyId,
        String companyName,
        UUID contactPersonId,
        String contactPersonName,
        UUID salesRepresentativeId,
        String salesRepresentativeName,
        UUID customerSiteId,
        String customerSiteName,
        String subject,
        String description,
        LocalDateTime plannedDate,
        LocalDateTime actualDate,
        MeetingEntity.MeetingStatus status,
        String outcome
) {
}
