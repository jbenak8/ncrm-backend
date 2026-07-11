package cz.jbenak.ncrm_backend.services;

import cz.jbenak.ncrm_backend.model.dto.company.SalesRepresentativeDto;
import cz.jbenak.ncrm_backend.model.dto.security.UserDto;
import cz.jbenak.ncrm_backend.model.entity.security.UserEntity;
import cz.jbenak.ncrm_backend.model.mapper.UserMapper;
import cz.jbenak.ncrm_backend.repository.SalesRepresentativeRepository;
import cz.jbenak.ncrm_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

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
    private final UserMapper userMapper;

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
}
