package cz.jbenak.ncrm_backend.model.mapper;

import cz.jbenak.ncrm_backend.configuration.MapstructConfig;
import cz.jbenak.ncrm_backend.model.dto.company.SalesRepresentativeDto;
import cz.jbenak.ncrm_backend.model.dto.company.SalesRepresentativeRequest;
import cz.jbenak.ncrm_backend.model.dto.security.RoleDto;
import cz.jbenak.ncrm_backend.model.dto.security.UserDto;
import cz.jbenak.ncrm_backend.model.dto.security.UserRequest;
import cz.jbenak.ncrm_backend.model.entity.company.CompanyEntity;
import cz.jbenak.ncrm_backend.model.entity.company.SalesRepresentativeEntity;
import cz.jbenak.ncrm_backend.model.entity.security.RoleEntity;
import cz.jbenak.ncrm_backend.model.entity.security.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-11
 * MapStruct mapper for users and sales representatives. The password hash is never mapped to any DTO.
 */
@Mapper(config = MapstructConfig.class)
public interface UserMapper {

    @Mapping(target = "companyIds", source = "companies")
    UserDto toDto(UserEntity entity);

    List<UserDto> toDtoList(List<UserEntity> entities);

    RoleDto toRoleDto(RoleEntity entity);

    List<RoleDto> toRoleDtoList(List<RoleEntity> entities);

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "firstName", source = "user.firstName")
    @Mapping(target = "lastName", source = "user.lastName")
    SalesRepresentativeDto toDto(SalesRepresentativeEntity entity);

    List<SalesRepresentativeDto> toRepresentativeDtoList(List<SalesRepresentativeEntity> entities);

    // The password hash, roles and assigned companies are resolved by the service; audit and login metadata are managed elsewhere.
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "companies", ignore = true)
    @Mapping(target = "credentialsExpired", ignore = true)
    @Mapping(target = "lastLoginAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    UserEntity toEntity(UserRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "companies", ignore = true)
    @Mapping(target = "credentialsExpired", ignore = true)
    @Mapping(target = "lastLoginAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(UserRequest request, @MappingTarget UserEntity entity);

    // The linked user account is resolved by the service.
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    SalesRepresentativeEntity toEntity(SalesRepresentativeRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(SalesRepresentativeRequest request, @MappingTarget SalesRepresentativeEntity entity);

    default Set<String> mapRoles(Set<RoleEntity> roles) {
        return roles == null ? Set.of() : roles.stream().map(RoleEntity::getName).collect(Collectors.toSet());
    }

    default Set<UUID> mapCompanyIds(Set<CompanyEntity> companies) {
        return companies == null ? Set.of() : companies.stream().map(CompanyEntity::getId).collect(Collectors.toSet());
    }
}
