package cz.jbenak.ncrm_backend.services;

import cz.jbenak.ncrm_backend.model.dto.store.ItemCategoryDto;
import cz.jbenak.ncrm_backend.model.dto.store.ItemDto;
import cz.jbenak.ncrm_backend.model.mapper.ItemMapper;
import cz.jbenak.ncrm_backend.repository.ItemCategoryRepository;
import cz.jbenak.ncrm_backend.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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

    private final ItemRepository itemRepository;
    private final ItemCategoryRepository itemCategoryRepository;
    private final ItemMapper itemMapper;

    public List<ItemDto> findAllActive() {
        return itemMapper.toDtoList(itemRepository.findAllByActiveTrue());
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
