package cz.jbenak.ncrm_backend.model.dto.customer;

import cz.jbenak.ncrm_backend.model.entity.customer.MeetingEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-11
 * Request DTO for planning or updating a meeting, including writing the meeting minutes (outcome).
 */
public record MeetingRequest(
        @NotNull UUID customerId,
        UUID companyId,
        UUID contactPersonId,
        @NotNull UUID salesRepresentativeId,
        UUID customerSiteId,
        @NotBlank String subject,
        String description,
        @NotNull LocalDateTime plannedDate,
        LocalDateTime actualDate,
        MeetingEntity.MeetingStatus status,
        String outcome
) {
}
