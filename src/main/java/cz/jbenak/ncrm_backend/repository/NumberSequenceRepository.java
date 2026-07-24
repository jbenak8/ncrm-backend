package cz.jbenak.ncrm_backend.repository;

import cz.jbenak.ncrm_backend.model.entity.NumberSequenceEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-16
 * Repository for number sequence definitions. The locking variant of the lookup by type is used
 * when drawing the next number so that concurrent transactions never obtain the same value.
 */
public interface NumberSequenceRepository extends JpaRepository<NumberSequenceEntity, UUID> {

    Optional<NumberSequenceEntity> findByType(NumberSequenceEntity.SequenceType type);

    boolean existsByType(NumberSequenceEntity.SequenceType type);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from NumberSequenceEntity s where s.type = :type")
    Optional<NumberSequenceEntity> findByTypeForUpdate(@Param("type") NumberSequenceEntity.SequenceType type);
}
