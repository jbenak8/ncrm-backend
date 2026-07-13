package cz.jbenak.ncrm_backend.services;

import cz.jbenak.ncrm_backend.model.dto.admin.CountryDto;
import cz.jbenak.ncrm_backend.model.entity.CountryEntity;
import cz.jbenak.ncrm_backend.model.mapper.CountryMapper;
import cz.jbenak.ncrm_backend.repository.CountryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-13
 * Service for country administration by the owner. Countries are identified by their ISO code,
 * are never physically deleted and can only be deactivated so that historic data stays consistent.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CountryService {

    private final CountryRepository countryRepository;
    private final CountryMapper countryMapper;

    @Transactional(readOnly = true)
    public List<CountryDto> findAll(boolean activeOnly) {
        return countryMapper.toDtoList(activeOnly ? countryRepository.findAllByActiveTrue() : countryRepository.findAll());
    }

    @Transactional(readOnly = true)
    public CountryDto findByIsoCode(String isoCode) {
        return countryMapper.toDto(getCountry(isoCode));
    }

    public CountryDto create(CountryDto request) {
        if (countryRepository.existsById(request.isoCode())) {
            throw new IllegalStateException("Country with ISO code " + request.isoCode() + " already exists");
        }
        CountryEntity saved = countryRepository.save(countryMapper.toEntity(request));
        log.info("Created country {} ({})", saved.getIsoCode(), saved.getName());
        return countryMapper.toDto(saved);
    }

    public CountryDto update(String isoCode, CountryDto request) {
        CountryEntity entity = getCountry(isoCode);
        countryMapper.updateEntity(request, entity);
        log.info("Updating country {} ({})", isoCode, request.name());
        return countryMapper.toDto(countryRepository.save(entity));
    }

    public CountryDto setActive(String isoCode, boolean active) {
        CountryEntity entity = getCountry(isoCode);
        entity.setActive(active);
        log.info("Country {} is now {}", isoCode, active ? "active" : "inactive");
        return countryMapper.toDto(countryRepository.save(entity));
    }

    private CountryEntity getCountry(String isoCode) {
        return countryRepository.findById(isoCode)
                .orElseThrow(() -> new NotFoundException("Country " + isoCode + " not found"));
    }
}
