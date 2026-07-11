package cz.jbenak.ncrm_backend.model.mapper;

import cz.jbenak.ncrm_backend.configuration.MapstructConfig;
import cz.jbenak.ncrm_backend.model.dto.company.SalesRepresentativeDto;
import cz.jbenak.ncrm_backend.model.dto.security.UserDto;
import cz.jbenak.ncrm_backend.model.entity.company.SalesRepresentativeEntity;
import cz.jbenak.ncrm_backend.model.entity.security.RoleEntity;
import cz.jbenak.ncrm_backend.model.entity.security.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-11
 * MapStruct mapper for users and sales representatives. The password hash is never mapped to any DTO.
 */
@Mapper(config = MapstructConfig.class)
public interface UserMapper {

    UserDto toDto(UserEntity entity);

    List<UserDto> toDtoList(List<UserEntity> entities);

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "firstName", source = "user.firstName")
    @Mapping(target = "lastName", source = "user.lastName")
    SalesRepresentativeDto toDto(SalesRepresentativeEntity entity);

    List<SalesRepresentativeDto> toRepresentativeDtoList(List<SalesRepresentativeEntity> entities);

    default Set<String> mapRoles(Set<RoleEntity> roles) {
        return roles == null ? Set.of() : roles.stream().map(RoleEntity::getName).collect(Collectors.toSet());
    }
}
