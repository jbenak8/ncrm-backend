package cz.jbenak.ncrm_backend.model.mapper;

import cz.jbenak.ncrm_backend.configuration.MapstructConfig;
import cz.jbenak.ncrm_backend.model.dto.admin.NumberSequenceDto;
import cz.jbenak.ncrm_backend.model.dto.admin.NumberSequenceRequest;
import cz.jbenak.ncrm_backend.model.entity.NumberSequenceEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-16
 * MapStruct mapper for number sequence definitions. The preview of the next generated number
 * is computed by the calling service and passed in explicitly.
 */
@Mapper(config = MapstructConfig.class)
public interface NumberSequenceMapper {

    @Mapping(target = "nextNumberPreview", source = "nextNumberPreview")
    NumberSequenceDto toDto(NumberSequenceEntity entity, String nextNumberPreview);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "lastResetYear", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    NumberSequenceEntity toEntity(NumberSequenceRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "lastResetYear", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(NumberSequenceRequest request, @MappingTarget NumberSequenceEntity entity);
}
