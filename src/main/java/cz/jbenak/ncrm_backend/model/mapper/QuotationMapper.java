package cz.jbenak.ncrm_backend.model.mapper;

import cz.jbenak.ncrm_backend.configuration.MapstructConfig;
import cz.jbenak.ncrm_backend.model.dto.quotation.QuotationDto;
import cz.jbenak.ncrm_backend.model.dto.quotation.QuotationItemDto;
import cz.jbenak.ncrm_backend.model.entity.quotation.QuotationEntity;
import cz.jbenak.ncrm_backend.model.entity.quotation.QuotationItemEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-22
 * MapStruct mapper for price quotations and their items. Entities are created by the service
 * because item prices are resolved server-side.
 */
@Mapper(config = MapstructConfig.class)
public interface QuotationMapper {

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
    @Mapping(target = "orderId", source = "order.id")
    @Mapping(target = "orderNumber", source = "order.orderNumber")
    QuotationDto toDto(QuotationEntity entity);

    List<QuotationDto> toDtoList(List<QuotationEntity> entities);

    @Mapping(target = "itemId", source = "item.id")
    @Mapping(target = "itemCode", source = "item.code")
    @Mapping(target = "itemName", source = "item.name")
    QuotationItemDto toDto(QuotationItemEntity entity);
}
