package cz.jbenak.ncrm_backend.model.mapper;

import cz.jbenak.ncrm_backend.configuration.MapstructConfig;
import cz.jbenak.ncrm_backend.model.dto.customer.MeetingDto;
import cz.jbenak.ncrm_backend.model.entity.customer.MeetingEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-11
 * MapStruct mapper for meetings. Entities are created and updated by the service
 * because all references must be resolved from repositories.
 */
@Mapper(config = MapstructConfig.class)
public interface MeetingMapper {

    @Mapping(target = "customerId", source = "customer.id")
    @Mapping(target = "customerName", source = "customer.name")
    @Mapping(target = "companyId", source = "company.id")
    @Mapping(target = "companyName", source = "company.name")
    @Mapping(target = "contactPersonId", source = "contactPerson.id")
    @Mapping(target = "contactPersonName",
            expression = "java(entity.getContactPerson() == null ? null : entity.getContactPerson().getFirstName() + \" \" + entity.getContactPerson().getLastName())")
    @Mapping(target = "salesRepresentativeId", source = "salesRepresentative.id")
    @Mapping(target = "salesRepresentativeName",
            expression = "java(entity.getSalesRepresentative() == null ? null : entity.getSalesRepresentative().getUser().getFirstName() + \" \" + entity.getSalesRepresentative().getUser().getLastName())")
    @Mapping(target = "customerSiteId", source = "customerSite.id")
    @Mapping(target = "customerSiteName", source = "customerSite.name")
    MeetingDto toDto(MeetingEntity entity);

    List<MeetingDto> toDtoList(List<MeetingEntity> entities);
}
