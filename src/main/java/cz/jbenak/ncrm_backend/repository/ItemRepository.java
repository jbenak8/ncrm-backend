package cz.jbenak.ncrm_backend.repository;

import cz.jbenak.ncrm_backend.model.entity.store.ItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-11
 * Repository for items (goods and services).
 */
@Repository
public interface ItemRepository extends JpaRepository<ItemEntity, UUID> {

    Optional<ItemEntity> findByCode(String code);

    List<ItemEntity> findAllByActiveTrue();

    List<ItemEntity> findAllByCategoryId(UUID categoryId);
}
