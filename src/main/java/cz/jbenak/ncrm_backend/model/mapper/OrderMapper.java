package cz.jbenak.ncrm_backend.model.mapper;

import cz.jbenak.ncrm_backend.configuration.MapstructConfig;
import cz.jbenak.ncrm_backend.model.dto.order.OrderDto;
import cz.jbenak.ncrm_backend.model.dto.order.OrderItemDto;
import cz.jbenak.ncrm_backend.model.entity.order.OrderEntity;
import cz.jbenak.ncrm_backend.model.entity.order.OrderItemEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-11
 * MapStruct mapper for orders and their items. Entities are created by the service
 * because item prices are resolved server-side.
 */
@Mapper(config = MapstructConfig.class)
public interface OrderMapper {

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
    OrderDto toDto(OrderEntity entity);

    List<OrderDto> toDtoList(List<OrderEntity> entities);

    @Mapping(target = "itemId", source = "item.id")
    @Mapping(target = "itemCode", source = "item.code")
    @Mapping(target = "itemName", source = "item.name")
    OrderItemDto toDto(OrderItemEntity entity);
}
