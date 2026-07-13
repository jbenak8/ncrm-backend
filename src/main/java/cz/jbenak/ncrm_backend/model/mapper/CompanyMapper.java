package cz.jbenak.ncrm_backend.model.mapper;

import cz.jbenak.ncrm_backend.configuration.MapstructConfig;
import cz.jbenak.ncrm_backend.model.dto.company.CompanyDto;
import cz.jbenak.ncrm_backend.model.dto.company.CompanyRequest;
import cz.jbenak.ncrm_backend.model.entity.company.CompanyEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-13
 * MapStruct mapper for own companies. The calling service resolves the address country reference
 * and manages the soft-delete metadata.
 */
@Mapper(config = MapstructConfig.class, uses = AddressMapper.class)
public interface CompanyMapper {

    @Mapping(target = "hasLogo", expression = "java(entity.getLogo() != null)")
    CompanyDto toDto(CompanyEntity entity);

    List<CompanyDto> toDtoList(List<CompanyEntity> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "address", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    @Mapping(target = "logo", ignore = true)
    @Mapping(target = "logoContentType", ignore = true)
    CompanyEntity toEntity(CompanyRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "address", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    @Mapping(target = "logo", ignore = true)
    @Mapping(target = "logoContentType", ignore = true)
    void updateEntity(CompanyRequest request, @MappingTarget CompanyEntity entity);
}
