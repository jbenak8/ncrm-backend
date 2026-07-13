package cz.jbenak.ncrm_backend.services;

import cz.jbenak.ncrm_backend.model.dto.admin.CountryDto;
import cz.jbenak.ncrm_backend.model.entity.CountryEntity;
import cz.jbenak.ncrm_backend.model.mapper.CountryMapper;
import cz.jbenak.ncrm_backend.repository.CountryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests of {@link CountryService} covering listing, creation with duplicate protection,
 * updates and activation / deactivation of countries.
 */
@ExtendWith(MockitoExtension.class)
class CountryServiceTest {

    @Mock
    private CountryRepository countryRepository;
    @Mock
    private CountryMapper countryMapper;

    @InjectMocks
    private CountryService countryService;

    private CountryDto request() {
        return new CountryDto("CZE", "Česko", "Czechia", "Česká republika", true, true, false, "CZ", "420");
    }

    @Test
    void findAllReturnsAllCountries() {
        when(countryRepository.findAll()).thenReturn(List.of());

        countryService.findAll(false);

        verify(countryRepository).findAll();
    }

    @Test
    void findAllActiveOnlyUsesActiveQuery() {
        when(countryRepository.findAllByActiveTrue()).thenReturn(List.of());

        countryService.findAll(true);

        verify(countryRepository).findAllByActiveTrue();
    }

    @Test
    void findByIsoCodeThrowsWhenMissing() {
        when(countryRepository.findById("XXX")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> countryService.findByIsoCode("XXX"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("XXX");
    }

    @Test
    void createRejectsDuplicateIsoCode() {
        when(countryRepository.existsById("CZE")).thenReturn(true);

        assertThatThrownBy(() -> countryService.create(request()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CZE");
        verify(countryRepository, never()).save(new CountryEntity());
    }

    @Test
    void createSavesNewCountry() {
        CountryEntity entity = new CountryEntity();
        when(countryRepository.existsById("CZE")).thenReturn(false);
        when(countryMapper.toEntity(request())).thenReturn(entity);
        when(countryRepository.save(entity)).thenReturn(entity);

        countryService.create(request());

        verify(countryRepository).save(entity);
    }

    @Test
    void updateAppliesChangesToExistingCountry() {
        CountryEntity entity = new CountryEntity();
        when(countryRepository.findById("CZE")).thenReturn(Optional.of(entity));
        when(countryRepository.save(entity)).thenReturn(entity);

        countryService.update("CZE", request());

        verify(countryMapper).updateEntity(request(), entity);
    }

    @Test
    void setActiveTogglesFlag() {
        CountryEntity entity = new CountryEntity();
        entity.setActive(true);
        when(countryRepository.findById("CZE")).thenReturn(Optional.of(entity));
        when(countryRepository.save(entity)).thenReturn(entity);

        countryService.setActive("CZE", false);

        assertThat(entity.isActive()).isFalse();
    }
}
