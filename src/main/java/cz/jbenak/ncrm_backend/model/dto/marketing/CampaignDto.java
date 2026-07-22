package cz.jbenak.ncrm_backend.model.dto.marketing;

import cz.jbenak.ncrm_backend.model.entity.marketing.CampaignEntity;
import cz.jbenak.ncrm_backend.model.entity.marketing.CampaignRecipientEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-11
 * DTO representing a marketing e-mail campaign with its recipients.
 */
public record CampaignDto(
        UUID id,
        String name,
        String subject,
        String body,
        CampaignEntity.ContentSource contentSource,
        CampaignEntity.CampaignStatus status,
        UUID companyId,
        String companyName,
        UUID createdById,
        String createdByName,
        LocalDateTime scheduledAt,
        LocalDateTime sentAt,
        List<RecipientDto> recipients
) {

    /**
     * DTO representing a single recipient of the campaign with its delivery status.
     */
    public record RecipientDto(
            UUID id,
            UUID customerId,
            String customerName,
            String email,
            CampaignRecipientEntity.DeliveryStatus deliveryStatus,
            LocalDateTime sentAt,
            String errorMessage
    ) {
    }
}
