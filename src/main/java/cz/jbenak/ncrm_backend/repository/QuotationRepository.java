package cz.jbenak.ncrm_backend.repository;

import cz.jbenak.ncrm_backend.model.entity.quotation.QuotationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-22
 * Repository for price quotations.
 */
@Repository
public interface QuotationRepository extends JpaRepository<QuotationEntity, UUID>, JpaSpecificationExecutor<QuotationEntity> {

    Optional<QuotationEntity> findByQuotationNumber(String quotationNumber);

    List<QuotationEntity> findAllByCustomerId(UUID customerId);

    List<QuotationEntity> findAllBySalesRepresentativeId(UUID salesRepresentativeId);

    List<QuotationEntity> findAllByStatus(QuotationEntity.QuotationStatus status);
}
