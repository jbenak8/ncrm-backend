package cz.jbenak.ncrm_backend.services;

import cz.jbenak.ncrm_backend.model.dto.admin.NumberSequenceDto;
import cz.jbenak.ncrm_backend.model.dto.admin.NumberSequenceRequest;
import cz.jbenak.ncrm_backend.model.entity.NumberSequenceEntity;
import cz.jbenak.ncrm_backend.model.mapper.NumberSequenceMapper;
import cz.jbenak.ncrm_backend.repository.NumberSequenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-16
 * Service for administration of number sequences (order and invoice numbers) and for drawing
 * the next number of a sequence. Drawing uses a pessimistic database lock so that concurrent
 * transactions never obtain the same number. A generated number consists of an optional prefix,
 * an optional year part and a zero-padded counter, e.g. {@code OBJ-2026-000123}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class NumberSequenceService {

    private final NumberSequenceRepository numberSequenceRepository;
    private final NumberSequenceMapper numberSequenceMapper;

    @Transactional(readOnly = true)
    public List<NumberSequenceDto> findAll() {
        return numberSequenceRepository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public NumberSequenceDto findById(UUID id) {
        return toDto(getSequence(id));
    }

    @Transactional(readOnly = true)
    public NumberSequenceDto findByType(NumberSequenceEntity.SequenceType type) {
        return toDto(numberSequenceRepository.findByType(type)
                .orElseThrow(() -> new NotFoundException("NumberSequence of type " + type + " not found")));
    }

    public NumberSequenceDto create(NumberSequenceRequest request) {
        if (numberSequenceRepository.existsByType(request.type())) {
            throw new IllegalStateException("Number sequence of type " + request.type() + " already exists");
        }
        NumberSequenceEntity saved = numberSequenceRepository.save(numberSequenceMapper.toEntity(request));
        log.info("Created number sequence {} with prefix '{}' and id {}",
                saved.getType(), saved.getPrefix(), saved.getId());
        return toDto(saved);
    }

    public NumberSequenceDto update(UUID id, NumberSequenceRequest request) {
        NumberSequenceEntity entity = getSequence(id);
        if (entity.getType() != request.type() && numberSequenceRepository.existsByType(request.type())) {
            throw new IllegalStateException("Number sequence of type " + request.type() + " already exists");
        }
        numberSequenceMapper.updateEntity(request, entity);
        log.info("Updating number sequence {} ({})", id, request.type());
        return toDto(numberSequenceRepository.save(entity));
    }

    public void delete(UUID id) {
        NumberSequenceEntity entity = getSequence(id);
        numberSequenceRepository.delete(entity);
        log.info("Deleted number sequence {} ({})", id, entity.getType());
    }

    /**
     * Draws the next number of the given sequence and advances its counter. The sequence row is
     * locked for the duration of the transaction, therefore two concurrent callers can never
     * obtain the same number. When the yearly reset is enabled and the year has changed since the
     * last draw, the counter is restarted from 1.
     *
     * @throws NotFoundException when no sequence of the given type is defined
     */
    public String nextNumber(NumberSequenceEntity.SequenceType type) {
        NumberSequenceEntity sequence = numberSequenceRepository.findByTypeForUpdate(type)
                .orElseThrow(() -> new NotFoundException("NumberSequence of type " + type + " not found"));
        int currentYear = LocalDate.now(ZoneId.systemDefault()).getYear();
        if (sequence.isYearlyReset() && sequence.getLastResetYear() != null
                && sequence.getLastResetYear() != currentYear) {
            log.info("Resetting number sequence {} counter for year {}", type, currentYear);
            sequence.setNextValue(1L);
        }
        sequence.setLastResetYear(currentYear);
        String number = format(sequence, sequence.getNextValue());
        sequence.setNextValue(sequence.getNextValue() + 1);
        numberSequenceRepository.save(sequence);
        log.debug("Generated number {} from sequence {}", number, type);
        return number;
    }

    /**
     * Draws the next number if a sequence of the given type is defined, otherwise returns an
     * empty {@link Optional} so that the caller can fall back to its own numbering scheme.
     */
    public Optional<String> tryNextNumber(NumberSequenceEntity.SequenceType type) {
        return numberSequenceRepository.existsByType(type) ? Optional.of(nextNumber(type)) : Optional.empty();
    }

    private NumberSequenceEntity getSequence(UUID id) {
        return numberSequenceRepository.findById(id).orElseThrow(() -> new NotFoundException("NumberSequence", id));
    }

    private NumberSequenceDto toDto(NumberSequenceEntity entity) {
        return numberSequenceMapper.toDto(entity, format(entity, entity.getNextValue()));
    }

    private String format(NumberSequenceEntity sequence, long value) {
        StringBuilder number = new StringBuilder();
        if (sequence.getPrefix() != null) {
            number.append(sequence.getPrefix());
        }
        if (sequence.isIncludeYear()) {
            number.append(LocalDate.now(ZoneId.systemDefault()).getYear()).append('-');
        }
        String counter = Long.toString(value);
        number.append("0".repeat(Math.max(0, sequence.getPadding() - counter.length()))).append(counter);
        return number.toString();
    }
}
