package cz.jbenak.ncrm_backend.repository;

import cz.jbenak.ncrm_backend.model.entity.invoice.InvoiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-16
 * Repository for issued invoices. One order can have at most one invoice, therefore the lookup
 * by order returns an {@link Optional}.
 */
public interface InvoiceRepository extends JpaRepository<InvoiceEntity, UUID> {

    Optional<InvoiceEntity> findByOrderId(UUID orderId);

    boolean existsByOrderId(UUID orderId);
}
