package cz.jbenak.ncrm_backend.repository;

import cz.jbenak.ncrm_backend.model.entity.store.ItemCategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-11
 * Repository for the hierarchical item category tree.
 */
@Repository
public interface ItemCategoryRepository extends JpaRepository<ItemCategoryEntity, UUID> {

    Optional<ItemCategoryEntity> findByCode(String code);

    List<ItemCategoryEntity> findAllByParentIsNullOrderBySortOrderAscNameAsc();

    List<ItemCategoryEntity> findAllByPathStartingWith(String pathPrefix);
}
