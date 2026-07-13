package cz.jbenak.ncrm_backend.model.mapper;

import cz.jbenak.ncrm_backend.configuration.MapstructConfig;
import cz.jbenak.ncrm_backend.model.dto.admin.VatRateDto;
import cz.jbenak.ncrm_backend.model.dto.admin.VatRateRequest;
import cz.jbenak.ncrm_backend.model.entity.VATRate;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-13
 * MapStruct mapper for VAT rates. The calling service resolves the country reference when mapping back to the entity.
 */
@Mapper(config = MapstructConfig.class)
public interface VatRateMapper {

    @Mapping(target = "countryIsoCode", source = "country.isoCode")
    @Mapping(target = "countryName", source = "country.name")
    VatRateDto toDto(VATRate entity);

    List<VatRateDto> toDtoList(List<VATRate> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "country", ignore = true)
    VATRate toEntity(VatRateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "country", ignore = true)
    void updateEntity(VatRateRequest request, @MappingTarget VATRate entity);
}
