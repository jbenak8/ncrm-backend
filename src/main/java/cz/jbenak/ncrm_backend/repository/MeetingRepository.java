package cz.jbenak.ncrm_backend.repository;

import cz.jbenak.ncrm_backend.model.entity.customer.MeetingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * @author Jan BenĂˇk
 * @version 1.0
 * @since 2026-07-11
 * Repository for meetings between sales representatives and customers.
 */
@Repository
public interface MeetingRepository extends JpaRepository<MeetingEntity, UUID>, JpaSpecificationExecutor<MeetingEntity> {

    List<MeetingEntity> findAllByCustomerId(UUID customerId);

    List<MeetingEntity> findAllBySalesRepresentativeId(UUID salesRepresentativeId);

    List<MeetingEntity> findAllByStatus(MeetingEntity.MeetingStatus status);

    List<MeetingEntity> findAllByPlannedDateBetween(LocalDateTime from, LocalDateTime to);

    long countByStatus(MeetingEntity.MeetingStatus status);

    long countBySalesRepresentativeId(UUID salesRepresentativeId);
}
