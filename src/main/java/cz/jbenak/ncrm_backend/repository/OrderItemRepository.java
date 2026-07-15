package cz.jbenak.ncrm_backend.repository;

import cz.jbenak.ncrm_backend.model.entity.order.OrderItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-15
 * Repository for order lines, used to detect whether a catalogue item is referenced by any order.
 */
@Repository
public interface OrderItemRepository extends JpaRepository<OrderItemEntity, UUID> {

    boolean existsByItemId(UUID itemId);
}
