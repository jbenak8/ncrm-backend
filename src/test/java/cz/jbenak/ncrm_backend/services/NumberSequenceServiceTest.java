package cz.jbenak.ncrm_backend.services;

import cz.jbenak.ncrm_backend.model.dto.admin.NumberSequenceRequest;
import cz.jbenak.ncrm_backend.model.entity.NumberSequenceEntity;
import cz.jbenak.ncrm_backend.model.entity.NumberSequenceEntity.SequenceType;
import cz.jbenak.ncrm_backend.model.mapper.NumberSequenceMapper;
import cz.jbenak.ncrm_backend.repository.NumberSequenceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests of {@link NumberSequenceService} covering CRUD validation, number generation
 * (prefix, year part, zero-padding), the yearly counter reset and the optional draw used
 * as a fallback by the order numbering.
 */
@ExtendWith(MockitoExtension.class)
class NumberSequenceServiceTest {

    @Mock
    private NumberSequenceRepository numberSequenceRepository;
    @Mock
    private NumberSequenceMapper numberSequenceMapper;

    @InjectMocks
    private NumberSequenceService numberSequenceService;

    private static final int CURRENT_YEAR = LocalDate.now(ZoneId.systemDefault()).getYear();

    private NumberSequenceEntity sequence(String prefix, boolean includeYear, int padding, long nextValue) {
        NumberSequenceEntity entity = new NumberSequenceEntity();
        entity.setId(UUID.randomUUID());
        entity.setType(SequenceType.ORDER);
        entity.setPrefix(prefix);
        entity.setIncludeYear(includeYear);
        entity.setPadding(padding);
        entity.setNextValue(nextValue);
        entity.setYearlyReset(true);
        return entity;
    }

    private NumberSequenceRequest request(SequenceType type) {
        return new NumberSequenceRequest(type, "OBJ-", true, 6, 1L, true, null);
    }

    @Test
    void createRejectsDuplicateType() {
        when(numberSequenceRepository.existsByType(SequenceType.ORDER)).thenReturn(true);

        NumberSequenceRequest request = request(SequenceType.ORDER);
        assertThatThrownBy(() -> numberSequenceService.create(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already exists");
        verify(numberSequenceRepository, never()).save(any());
    }

    @Test
    void createSavesMappedEntity() {
        NumberSequenceEntity entity = sequence("OBJ-", true, 6, 1L);
        when(numberSequenceRepository.existsByType(SequenceType.ORDER)).thenReturn(false);
        when(numberSequenceMapper.toEntity(any(NumberSequenceRequest.class))).thenReturn(entity);
        when(numberSequenceRepository.save(entity)).thenReturn(entity);

        numberSequenceService.create(request(SequenceType.ORDER));

        verify(numberSequenceRepository).save(entity);
        verify(numberSequenceMapper).toDto(entity, "OBJ-" + CURRENT_YEAR + "-000001");
    }

    @Test
    void updateRejectsTypeChangeToExistingType() {
        UUID id = UUID.randomUUID();
        NumberSequenceEntity entity = sequence("OBJ-", true, 6, 1L);
        when(numberSequenceRepository.findById(id)).thenReturn(Optional.of(entity));
        when(numberSequenceRepository.existsByType(SequenceType.INVOICE)).thenReturn(true);

        NumberSequenceRequest request = request(SequenceType.INVOICE);
        assertThatThrownBy(() -> numberSequenceService.update(id, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already exists");
        verify(numberSequenceRepository, never()).save(any());
    }

    @Test
    void findByIdThrowsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(numberSequenceRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> numberSequenceService.findById(id))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("NumberSequence");
    }

    @Test
    void nextNumberFormatsAndAdvancesCounter() {
        NumberSequenceEntity entity = sequence("OBJ-", true, 6, 123L);
        entity.setLastResetYear(CURRENT_YEAR);
        when(numberSequenceRepository.findByTypeForUpdate(SequenceType.ORDER)).thenReturn(Optional.of(entity));
        when(numberSequenceRepository.save(entity)).thenReturn(entity);

        String number = numberSequenceService.nextNumber(SequenceType.ORDER);

        assertThat(number).isEqualTo("OBJ-" + CURRENT_YEAR + "-000123");
        assertThat(entity.getNextValue()).isEqualTo(124L);
        verify(numberSequenceRepository).save(entity);
    }

    @Test
    void nextNumberWithoutPrefixAndYearUsesPaddedCounterOnly() {
        NumberSequenceEntity entity = sequence(null, false, 4, 7L);
        entity.setLastResetYear(CURRENT_YEAR);
        when(numberSequenceRepository.findByTypeForUpdate(SequenceType.ORDER)).thenReturn(Optional.of(entity));
        when(numberSequenceRepository.save(entity)).thenReturn(entity);

        assertThat(numberSequenceService.nextNumber(SequenceType.ORDER)).isEqualTo("0007");
    }

    @Test
    void nextNumberResetsCounterOnNewYear() {
        NumberSequenceEntity entity = sequence("FA-", true, 6, 999L);
        entity.setLastResetYear(CURRENT_YEAR - 1);
        when(numberSequenceRepository.findByTypeForUpdate(SequenceType.ORDER)).thenReturn(Optional.of(entity));
        when(numberSequenceRepository.save(entity)).thenReturn(entity);

        String number = numberSequenceService.nextNumber(SequenceType.ORDER);

        assertThat(number).isEqualTo("FA-" + CURRENT_YEAR + "-000001");
        assertThat(entity.getNextValue()).isEqualTo(2L);
        assertThat(entity.getLastResetYear()).isEqualTo(CURRENT_YEAR);
    }

    @Test
    void nextNumberDoesNotResetWhenYearlyResetDisabled() {
        NumberSequenceEntity entity = sequence("FA-", false, 6, 999L);
        entity.setYearlyReset(false);
        entity.setLastResetYear(CURRENT_YEAR - 1);
        when(numberSequenceRepository.findByTypeForUpdate(SequenceType.ORDER)).thenReturn(Optional.of(entity));
        when(numberSequenceRepository.save(entity)).thenReturn(entity);

        assertThat(numberSequenceService.nextNumber(SequenceType.ORDER)).isEqualTo("FA-000999");
        assertThat(entity.getNextValue()).isEqualTo(1000L);
    }

    @Test
    void nextNumberThrowsWhenSequenceNotDefined() {
        when(numberSequenceRepository.findByTypeForUpdate(SequenceType.INVOICE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> numberSequenceService.nextNumber(SequenceType.INVOICE))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("INVOICE");
    }

    @Test
    void tryNextNumberReturnsEmptyWhenSequenceNotDefined() {
        when(numberSequenceRepository.existsByType(SequenceType.ORDER)).thenReturn(false);

        assertThat(numberSequenceService.tryNextNumber(SequenceType.ORDER)).isEmpty();
        verify(numberSequenceRepository, never()).findByTypeForUpdate(any());
    }

    @Test
    void tryNextNumberDrawsWhenSequenceDefined() {
        NumberSequenceEntity entity = sequence("OBJ-", false, 3, 5L);
        entity.setLastResetYear(CURRENT_YEAR);
        when(numberSequenceRepository.existsByType(SequenceType.ORDER)).thenReturn(true);
        when(numberSequenceRepository.findByTypeForUpdate(SequenceType.ORDER)).thenReturn(Optional.of(entity));
        when(numberSequenceRepository.save(entity)).thenReturn(entity);

        assertThat(numberSequenceService.tryNextNumber(SequenceType.ORDER)).contains("OBJ-005");
    }

    @Test
    void deleteRemovesSequence() {
        UUID id = UUID.randomUUID();
        NumberSequenceEntity entity = sequence("OBJ-", true, 6, 1L);
        when(numberSequenceRepository.findById(id)).thenReturn(Optional.of(entity));

        numberSequenceService.delete(id);

        verify(numberSequenceRepository).delete(entity);
    }
}
