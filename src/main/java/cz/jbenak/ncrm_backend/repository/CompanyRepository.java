package cz.jbenak.ncrm_backend.repository;

import cz.jbenak.ncrm_backend.model.entity.company.CompanyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-13
 * Repository for own companies of the system owner. Deleted companies are only soft-deleted.
 */
@Repository
public interface CompanyRepository extends JpaRepository<CompanyEntity, UUID> {

    List<CompanyEntity> findAllByDeletedFalse();

    Optional<CompanyEntity> findByDefaultCompanyTrueAndDeletedFalse();

    boolean existsByRegistrationId(String registrationId);
}
