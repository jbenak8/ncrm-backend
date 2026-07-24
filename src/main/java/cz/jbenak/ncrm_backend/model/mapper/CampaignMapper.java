package cz.jbenak.ncrm_backend.model.mapper;

import cz.jbenak.ncrm_backend.configuration.MapstructConfig;
import cz.jbenak.ncrm_backend.model.dto.marketing.CampaignDto;
import cz.jbenak.ncrm_backend.model.entity.marketing.CampaignEntity;
import cz.jbenak.ncrm_backend.model.entity.marketing.CampaignRecipientEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-11
 * MapStruct mapper for marketing campaigns and their recipients.
 */
@Mapper(config = MapstructConfig.class)
public interface CampaignMapper {

    @Mapping(target = "companyId", source = "company.id")
    @Mapping(target = "companyName", source = "company.name")
    @Mapping(target = "createdById", source = "createdBy.id")
    @Mapping(target = "createdByName",
            expression = "java(entity.getCreatedBy() == null ? null : entity.getCreatedBy().getFirstName() + \" \" + entity.getCreatedBy().getLastName())")
    CampaignDto toDto(CampaignEntity entity);

    List<CampaignDto> toDtoList(List<CampaignEntity> entities);

    @Mapping(target = "customerId", source = "customer.id")
    @Mapping(target = "customerName", source = "customer.name")
    CampaignDto.RecipientDto toDto(CampaignRecipientEntity entity);
}
