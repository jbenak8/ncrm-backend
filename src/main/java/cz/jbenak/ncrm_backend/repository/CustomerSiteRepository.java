package cz.jbenak.ncrm_backend.repository;

import cz.jbenak.ncrm_backend.model.entity.customer.CustomerSiteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-11
 * Repository for customer sites (branches, plants, warehouses).
 */
@Repository
public interface CustomerSiteRepository extends JpaRepository<CustomerSiteEntity, UUID> {

    List<CustomerSiteEntity> findAllByCustomerId(UUID customerId);
}
