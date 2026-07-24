package cz.jbenak.ncrm_backend.model.mapper;

import cz.jbenak.ncrm_backend.configuration.MapstructConfig;
import cz.jbenak.ncrm_backend.model.dto.customer.ContactPersonDto;
import cz.jbenak.ncrm_backend.model.dto.customer.CustomerDto;
import cz.jbenak.ncrm_backend.model.dto.customer.CustomerRequest;
import cz.jbenak.ncrm_backend.model.dto.customer.CustomerSiteDto;
import cz.jbenak.ncrm_backend.model.entity.customer.ContactPersonEntity;
import cz.jbenak.ncrm_backend.model.entity.customer.CustomerEntity;
import cz.jbenak.ncrm_backend.model.entity.customer.CustomerSiteEntity;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-11
 * MapStruct mapper for customers, their contact persons and sites.
 */
@Mapper(config = MapstructConfig.class, uses = AddressMapper.class)
public interface CustomerMapper {

    @Mapping(target = "salesRepresentativeId", source = "salesRepresentative.id")
    @Mapping(target = "salesRepresentativeName",
            expression = "java(entity.getSalesRepresentative() == null ? null : entity.getSalesRepresentative().getUser().getFirstName() + \" \" + entity.getSalesRepresentative().getUser().getLastName())")
    CustomerDto toDto(CustomerEntity entity);

    List<CustomerDto> toDtoList(List<CustomerEntity> entities);

    ContactPersonDto toDto(ContactPersonEntity entity);

    CustomerSiteDto toDto(CustomerSiteEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "salesRepresentative", ignore = true)
    @Mapping(target = "contactPersons", ignore = true)
    @Mapping(target = "sites", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    CustomerEntity toEntity(CustomerRequest request);

    @BeanMapping(ignoreByDefault = false)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "salesRepresentative", ignore = true)
    @Mapping(target = "contactPersons", ignore = true)
    @Mapping(target = "sites", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(CustomerRequest request, @MappingTarget CustomerEntity entity);
}
