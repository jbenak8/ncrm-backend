package cz.jbenak.ncrm_backend.services;

import cz.jbenak.ncrm_backend.model.dto.store.ItemCategoryDto;
import cz.jbenak.ncrm_backend.model.dto.store.ItemDto;
import cz.jbenak.ncrm_backend.model.mapper.ItemMapper;
import cz.jbenak.ncrm_backend.repository.ItemCategoryRepository;
import cz.jbenak.ncrm_backend.repository.ItemRepository;
import cz.jbenak.ncrm_backend.search.SearchSpecificationBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-11
 * Read service for the store catalogue (items and the category tree) used when creating orders.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemService {

    /** Attribute paths of the item entity that can be used by the generic search API. */
    private static final Set<String> SEARCHABLE_FIELDS = Set.of(
            "code", "name", "description", "itemType", "unit", "active",
            "category.id", "price.price", "price.currency", "price.vatRate");

    private final ItemRepository itemRepository;
    private final ItemCategoryRepository itemCategoryRepository;
    private final ItemMapper itemMapper;

    public List<ItemDto> findAllActive() {
        return itemMapper.toDtoList(itemRepository.findAllByActiveTrue());
    }

    /**
     * Generic search over catalogue items. Filters are raw {@code field:operator:value} expressions,
     * combined with a logical AND; see {@link SearchSpecificationBuilder}.
     */
    public Page<ItemDto> search(List<String> filters, Pageable pageable) {
        log.debug("Searching items with filters {}", filters);
        return itemRepository.findAll(SearchSpecificationBuilder.build(filters, SEARCHABLE_FIELDS), pageable)
                .map(itemMapper::toDto);
    }

    public ItemDto findById(UUID id) {
        log.debug("Looking up catalogue item {}", id);
        return itemRepository.findById(id).map(itemMapper::toDto)
                .orElseThrow(() -> new NotFoundException("Item", id));
    }

    public List<ItemDto> findByCategory(UUID categoryId) {
        return itemMapper.toDtoList(itemRepository.findAllByCategoryId(categoryId));
    }

    public List<ItemCategoryDto> findCategoryTree() {
        return itemMapper.toCategoryDtoList(itemCategoryRepository.findAllByParentIsNullOrderBySortOrderAscNameAsc());
    }
}
