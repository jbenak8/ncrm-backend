package cz.jbenak.ncrm_backend.repository;

import cz.jbenak.ncrm_backend.model.entity.marketing.CampaignEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * @author Jan BenĂˇk
 * @version 1.0
 * @since 2026-07-11
 * Repository for marketing e-mail campaigns.
 */
@Repository
public interface CampaignRepository extends JpaRepository<CampaignEntity, UUID>, JpaSpecificationExecutor<CampaignEntity> {

    List<CampaignEntity> findAllByStatus(CampaignEntity.CampaignStatus status);

    List<CampaignEntity> findAllByStatusInOrderByScheduledAtAscNameAsc(Collection<CampaignEntity.CampaignStatus> statuses);

    long countByStatus(CampaignEntity.CampaignStatus status);
}
