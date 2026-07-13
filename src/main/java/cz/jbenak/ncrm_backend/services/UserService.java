package cz.jbenak.ncrm_backend.services;

import cz.jbenak.ncrm_backend.model.dto.company.SalesRepresentativeDto;
import cz.jbenak.ncrm_backend.model.dto.security.UserDto;
import cz.jbenak.ncrm_backend.model.dto.security.UserRequest;
import cz.jbenak.ncrm_backend.model.entity.security.RoleEntity;
import cz.jbenak.ncrm_backend.model.entity.security.UserEntity;
import cz.jbenak.ncrm_backend.model.mapper.UserMapper;
import cz.jbenak.ncrm_backend.repository.RoleRepository;
import cz.jbenak.ncrm_backend.repository.SalesRepresentativeRepository;
import cz.jbenak.ncrm_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-11
 * Service for user administration. Authentication itself is delegated to Keycloak (OIDC);
 * this service only manages the local user projection and account state flags.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final SalesRepresentativeRepository salesRepresentativeRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<UserDto> findAll() {
        return userMapper.toDtoList(userRepository.findAll());
    }

    @Transactional(readOnly = true)
    public UserDto findByUsername(String username) {
        return userRepository.findByUsername(username).map(userMapper::toDto)
                .orElseThrow(() -> new NotFoundException("User " + username + " not found"));
    }

    @Transactional(readOnly = true)
    public List<SalesRepresentativeDto> findAllSalesRepresentatives() {
        return userMapper.toRepresentativeDtoList(salesRepresentativeRepository.findAllByActiveTrue());
    }

    public void recordLogin(String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            user.setLastLoginAt(LocalDateTime.now(ZoneId.systemDefault()));
            userRepository.save(user);
            log.debug("Recorded login of user {}", username);
        });
    }

    public UserDto create(UserRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new IllegalStateException("User with username " + request.username() + " already exists");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalStateException("User with e-mail " + request.email() + " already exists");
        }
        if (request.password() == null || request.password().isBlank()) {
            throw new IllegalStateException("Password is mandatory when creating a user");
        }
        UserEntity entity = userMapper.toEntity(request);
        entity.setPasswordHash(passwordEncoder.encode(request.password()));
        entity.setRoles(resolveRoles(request.roles()));
        UserEntity saved = userRepository.save(entity);
        log.info("Created user {} with id {}", request.username(), saved.getId());
        return userMapper.toDto(saved);
    }

    public UserDto update(UUID id, UserRequest request) {
        UserEntity entity = getUser(id);
        userRepository.findByUsername(request.username())
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new IllegalStateException("User with username " + request.username() + " already exists");
                });
        userRepository.findByEmail(request.email())
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new IllegalStateException("User with e-mail " + request.email() + " already exists");
                });
        userMapper.updateEntity(request, entity);
        if (request.password() != null && !request.password().isBlank()) {
            entity.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        entity.setRoles(resolveRoles(request.roles()));
        log.info("Updating user {} ({})", id, request.username());
        return userMapper.toDto(userRepository.save(entity));
    }

    public void delete(UUID id) {
        UserEntity entity = getUser(id);
        salesRepresentativeRepository.findByUserUsername(entity.getUsername()).ifPresent(rep -> {
            throw new IllegalStateException("User " + entity.getUsername()
                    + " is linked to sales representative " + rep.getCode() + " and cannot be deleted");
        });
        userRepository.delete(entity);
        log.info("Deleted user {} ({})", id, entity.getUsername());
    }

    public UserDto setLocked(UUID id, boolean locked) {
        UserEntity user = userRepository.findById(id).orElseThrow(() -> new NotFoundException("User", id));
        user.setLocked(locked);
        log.info("User {} ({}) is now {}", id, user.getUsername(), locked ? "locked" : "unlocked");
        return userMapper.toDto(userRepository.save(user));
    }

    public UserDto setEnabled(UUID id, boolean enabled) {
        UserEntity user = userRepository.findById(id).orElseThrow(() -> new NotFoundException("User", id));
        user.setEnabled(enabled);
        log.info("User {} ({}) is now {}", id, user.getUsername(), enabled ? "enabled" : "disabled");
        return userMapper.toDto(userRepository.save(user));
    }

    private UserEntity getUser(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> new NotFoundException("User", id));
    }

    private Set<RoleEntity> resolveRoles(Set<String> roleNames) {
        return roleNames == null ? new HashSet<>() : roleNames.stream()
                .map(name -> roleRepository.findByName(name)
                        .orElseThrow(() -> new NotFoundException("Role " + name + " not found")))
                .collect(Collectors.toCollection(HashSet::new));
    }
}
