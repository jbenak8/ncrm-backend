package cz.jbenak.ncrm_backend.services;

import cz.jbenak.ncrm_backend.model.dto.company.SalesRepresentativeDto;
import cz.jbenak.ncrm_backend.model.dto.company.SalesRepresentativeRequest;
import cz.jbenak.ncrm_backend.model.entity.company.SalesRepresentativeEntity;
import cz.jbenak.ncrm_backend.model.mapper.UserMapper;
import cz.jbenak.ncrm_backend.repository.SalesRepresentativeRepository;
import cz.jbenak.ncrm_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-13
 * Service for administration of sales representatives by the owner. Every representative is linked
 * to an existing user account; representatives are never deleted, only deactivated.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SalesRepresentativeService {

    private final SalesRepresentativeRepository salesRepresentativeRepository;
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public List<SalesRepresentativeDto> findAll() {
        return userMapper.toRepresentativeDtoList(salesRepresentativeRepository.findAll());
    }

    @Transactional(readOnly = true)
    public SalesRepresentativeDto findById(UUID id) {
        return userMapper.toDto(getRepresentative(id));
    }

    public SalesRepresentativeDto create(SalesRepresentativeRequest request) {
        if (salesRepresentativeRepository.findByCode(request.code()).isPresent()) {
            throw new IllegalStateException("Sales representative with code " + request.code() + " already exists");
        }
        SalesRepresentativeEntity entity = userMapper.toEntity(request);
        applyUser(entity, request);
        SalesRepresentativeEntity saved = salesRepresentativeRepository.save(entity);
        log.info("Created sales representative {} with id {}", request.code(), saved.getId());
        return userMapper.toDto(saved);
    }

    public SalesRepresentativeDto update(UUID id, SalesRepresentativeRequest request) {
        SalesRepresentativeEntity entity = getRepresentative(id);
        salesRepresentativeRepository.findByCode(request.code())
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new IllegalStateException("Sales representative with code " + request.code() + " already exists");
                });
        userMapper.updateEntity(request, entity);
        applyUser(entity, request);
        log.info("Updating sales representative {} ({})", id, request.code());
        return userMapper.toDto(salesRepresentativeRepository.save(entity));
    }

    public SalesRepresentativeDto setActive(UUID id, boolean active) {
        SalesRepresentativeEntity entity = getRepresentative(id);
        entity.setActive(active);
        log.info("Sales representative {} ({}) is now {}", id, entity.getCode(), active ? "active" : "inactive");
        return userMapper.toDto(salesRepresentativeRepository.save(entity));
    }

    private SalesRepresentativeEntity getRepresentative(UUID id) {
        return salesRepresentativeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("SalesRepresentative", id));
    }

    private void applyUser(SalesRepresentativeEntity entity, SalesRepresentativeRequest request) {
        entity.setUser(userRepository.findById(request.userId())
                .orElseThrow(() -> new NotFoundException("User", request.userId())));
    }
}
