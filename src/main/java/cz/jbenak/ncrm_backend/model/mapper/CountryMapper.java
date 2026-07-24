package cz.jbenak.ncrm_backend.model.mapper;

import cz.jbenak.ncrm_backend.configuration.MapstructConfig;
import cz.jbenak.ncrm_backend.model.dto.admin.CountryDto;
import cz.jbenak.ncrm_backend.model.entity.CountryEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-13
 * MapStruct mapper between {@link CountryEntity} and {@link CountryDto} for the country administration.
 */
@Mapper(config = MapstructConfig.class)
public interface CountryMapper {

    CountryDto toDto(CountryEntity entity);

    List<CountryDto> toDtoList(List<CountryEntity> entities);

    CountryEntity toEntity(CountryDto dto);

    // The ISO code is the primary key and must never be changed by an update.
    @Mapping(target = "isoCode", ignore = true)
    void updateEntity(CountryDto dto, @MappingTarget CountryEntity entity);
}
