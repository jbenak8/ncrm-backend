package cz.jbenak.ncrm_backend.repository;

import cz.jbenak.ncrm_backend.model.entity.customer.CustomerEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-11
 * Repository for customers.
 */
@Repository
public interface CustomerRepository extends JpaRepository<CustomerEntity, UUID> {

    Optional<CustomerEntity> findByRegistrationId(String registrationId);

    boolean existsByRegistrationId(String registrationId);

    List<CustomerEntity> findAllByActiveTrue();

    Page<CustomerEntity> findAllByNameContainingIgnoreCase(String name, Pageable pageable);

    List<CustomerEntity> findAllBySalesRepresentativeId(UUID salesRepresentativeId);

    long countByActiveTrue();
}
