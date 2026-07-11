package cz.jbenak.ncrm_backend.repository;

import cz.jbenak.ncrm_backend.model.entity.customer.ContactPersonEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-11
 * Repository for contact persons of customers.
 */
@Repository
public interface ContactPersonRepository extends JpaRepository<ContactPersonEntity, UUID> {

    List<ContactPersonEntity> findAllByCustomerId(UUID customerId);
}
