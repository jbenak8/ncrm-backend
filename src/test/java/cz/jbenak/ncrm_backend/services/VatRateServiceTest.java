package cz.jbenak.ncrm_backend.services;

import cz.jbenak.ncrm_backend.model.dto.admin.VatRateRequest;
import cz.jbenak.ncrm_backend.model.entity.CountryEntity;
import cz.jbenak.ncrm_backend.model.entity.VATRate;
import cz.jbenak.ncrm_backend.model.mapper.VatRateMapper;
import cz.jbenak.ncrm_backend.repository.CountryRepository;
import cz.jbenak.ncrm_backend.repository.VatRateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests of {@link VatRateService} covering listing, creation with country resolution,
 * validity period validation, updates and deletion of VAT rates.
 */
@ExtendWith(MockitoExtension.class)
class VatRateServiceTest {

    @Mock
    private VatRateRepository vatRateRepository;
    @Mock
    private CountryRepository countryRepository;
    @Mock
    private VatRateMapper vatRateMapper;

    @InjectMocks
    private VatRateService vatRateService;

    private VatRateRequest request(LocalDateTime validTo) {
        return new VatRateRequest("CZE", VATRate.VATType.BASE, new BigDecimal("21.00"),
                LocalDateTime.of(2026, Month.JANUARY, 1, 0, 0), validTo);
    }

    @Test
    void findAllDelegatesToRepository() {
        when(vatRateRepository.findAll()).thenReturn(List.of());

        vatRateService.findAll();

        verify(vatRateMapper).toDtoList(List.of());
    }

    @Test
    void findByCountryUsesCountryQuery() {
        when(vatRateRepository.findAllByCountryIsoCodeOrderByValidFromDesc("CZE")).thenReturn(List.of());

        vatRateService.findByCountry("CZE");

        verify(vatRateRepository).findAllByCountryIsoCodeOrderByValidFromDesc("CZE");
    }

    @Test
    void findByIdThrowsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(vatRateRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vatRateService.findById(id))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void createResolvesCountryReference() {
        VATRate entity = new VATRate();
        CountryEntity country = new CountryEntity();
        when(vatRateMapper.toEntity(any(VatRateRequest.class))).thenReturn(entity);
        when(countryRepository.findById("CZE")).thenReturn(Optional.of(country));
        when(vatRateRepository.save(entity)).thenReturn(entity);

        vatRateService.create(request(null));

        assertThat(entity.getCountry()).isSameAs(country);
        verify(vatRateRepository).save(entity);
    }

    @Test
    void createThrowsWhenCountryMissing() {
        when(vatRateMapper.toEntity(any(VatRateRequest.class))).thenReturn(new VATRate());
        when(countryRepository.findById("CZE")).thenReturn(Optional.empty());

        VatRateRequest request = request(null);
        assertThatThrownBy(() -> vatRateService.create(request))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("CZE");
    }

    @Test
    void createRejectsInvalidValidityPeriod() {
        VatRateRequest request = request(LocalDateTime.of(2025, Month.JANUARY, 1, 0, 0));
        assertThatThrownBy(() -> vatRateService.create(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("validity");
    }

    @Test
    void updateAppliesChangesAndCountry() {
        UUID id = UUID.randomUUID();
        VATRate entity = new VATRate();
        CountryEntity country = new CountryEntity();
        when(vatRateRepository.findById(id)).thenReturn(Optional.of(entity));
        when(countryRepository.findById("CZE")).thenReturn(Optional.of(country));
        when(vatRateRepository.save(entity)).thenReturn(entity);

        vatRateService.update(id, request(null));

        verify(vatRateMapper).updateEntity(request(null), entity);
        assertThat(entity.getCountry()).isSameAs(country);
    }

    @Test
    void deleteRemovesExistingRate() {
        UUID id = UUID.randomUUID();
        VATRate entity = new VATRate();
        when(vatRateRepository.findById(id)).thenReturn(Optional.of(entity));

        vatRateService.delete(id);

        verify(vatRateRepository).delete(entity);
    }
}
