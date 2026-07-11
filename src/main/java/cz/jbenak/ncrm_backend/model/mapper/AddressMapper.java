package cz.jbenak.ncrm_backend.model.mapper;

import cz.jbenak.ncrm_backend.configuration.MapstructConfig;
import cz.jbenak.ncrm_backend.model.dto.AddressDto;
import cz.jbenak.ncrm_backend.model.entity.AddressEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-11
 * MapStruct mapper between {@link AddressEntity} and {@link AddressDto}.
 * The calling service resolves the country reference when mapping back to the entity.
 */
@Mapper(config = MapstructConfig.class)
public interface AddressMapper {

    @Mapping(target = "countryIsoCode", source = "country.isoCode")
    @Mapping(target = "countryName", source = "country.name")
    AddressDto toDto(AddressEntity entity);

    @Mapping(target = "country", ignore = true)
    AddressEntity toEntity(AddressDto dto);
}
