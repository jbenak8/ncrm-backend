package cz.jbenak.ncrm_backend.services;

import cz.jbenak.ncrm_backend.model.dto.admin.VatRateDto;
import cz.jbenak.ncrm_backend.model.dto.admin.VatRateRequest;
import cz.jbenak.ncrm_backend.model.entity.VATRate;
import cz.jbenak.ncrm_backend.model.mapper.VatRateMapper;
import cz.jbenak.ncrm_backend.repository.CountryRepository;
import cz.jbenak.ncrm_backend.repository.VatRateRepository;
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
 * Service for VAT rate administration by the owner, including resolution of the country reference
 * and a validity period sanity check (validTo must not precede validFrom).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class VatRateService {

    private final VatRateRepository vatRateRepository;
    private final CountryRepository countryRepository;
    private final VatRateMapper vatRateMapper;

    @Transactional(readOnly = true)
    public List<VatRateDto> findAll() {
        return vatRateMapper.toDtoList(vatRateRepository.findAll());
    }

    @Transactional(readOnly = true)
    public List<VatRateDto> findByCountry(String countryIsoCode) {
        return vatRateMapper.toDtoList(vatRateRepository.findAllByCountryIsoCodeOrderByValidFromDesc(countryIsoCode));
    }

    @Transactional(readOnly = true)
    public VatRateDto findById(UUID id) {
        return vatRateMapper.toDto(getVatRate(id));
    }

    public VatRateDto create(VatRateRequest request) {
        validatePeriod(request);
        VATRate entity = vatRateMapper.toEntity(request);
        applyCountry(entity, request);
        VATRate saved = vatRateRepository.save(entity);
        log.info("Created VAT rate {} {} % for country {} with id {}",
                request.type(), request.rate(), request.countryIsoCode(), saved.getId());
        return vatRateMapper.toDto(saved);
    }

    public VatRateDto update(UUID id, VatRateRequest request) {
        validatePeriod(request);
        VATRate entity = getVatRate(id);
        vatRateMapper.updateEntity(request, entity);
        applyCountry(entity, request);
        log.info("Updating VAT rate {} ({} {} %)", id, request.type(), request.rate());
        return vatRateMapper.toDto(vatRateRepository.save(entity));
    }

    public void delete(UUID id) {
        VATRate entity = getVatRate(id);
        vatRateRepository.delete(entity);
        log.info("Deleted VAT rate {} ({} {} %)", id, entity.getType(), entity.getRate());
    }

    private VATRate getVatRate(UUID id) {
        return vatRateRepository.findById(id).orElseThrow(() -> new NotFoundException("VATRate", id));
    }

    private void applyCountry(VATRate entity, VatRateRequest request) {
        entity.setCountry(countryRepository.findById(request.countryIsoCode())
                .orElseThrow(() -> new NotFoundException("Country " + request.countryIsoCode() + " not found")));
    }

    private void validatePeriod(VatRateRequest request) {
        if (request.validTo() != null && request.validTo().isBefore(request.validFrom())) {
            throw new IllegalStateException("VAT rate validity end must not precede its start");
        }
    }
}
