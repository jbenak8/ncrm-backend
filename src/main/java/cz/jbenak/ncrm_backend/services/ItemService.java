package cz.jbenak.ncrm_backend.services;

import cz.jbenak.ncrm_backend.model.dto.store.ItemCategoryDeleteResult;
import cz.jbenak.ncrm_backend.model.dto.store.ItemCategoryDto;
import cz.jbenak.ncrm_backend.model.dto.store.ItemCategoryRequest;
import cz.jbenak.ncrm_backend.model.dto.store.ItemDeleteResult;
import cz.jbenak.ncrm_backend.model.dto.store.ItemDto;
import cz.jbenak.ncrm_backend.model.dto.store.ItemImageDto;
import cz.jbenak.ncrm_backend.model.dto.store.ItemRequest;
import cz.jbenak.ncrm_backend.model.entity.store.ItemCategoryEntity;
import cz.jbenak.ncrm_backend.model.entity.store.ItemEntity;
import cz.jbenak.ncrm_backend.model.entity.store.ItemPriceEntity;
import cz.jbenak.ncrm_backend.model.mapper.ItemMapper;
import cz.jbenak.ncrm_backend.repository.ItemCategoryRepository;
import cz.jbenak.ncrm_backend.repository.ItemRepository;
import cz.jbenak.ncrm_backend.repository.OrderItemRepository;
import cz.jbenak.ncrm_backend.search.SearchSpecificationBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-11
 * Service for the store catalogue (items and the category tree). Provides read operations used when
 * creating orders as well as administration of items and categories (create / update / delete).
 * Items referenced by orders are never removed physically, they are only deactivated.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemService {

    private static final Set<String> ALLOWED_IMAGE_CONTENT_TYPES =
            Set.of("image/png", "image/jpeg", "image/gif", "image/webp", "image/svg+xml");
    private static final long MAX_IMAGE_SIZE_BYTES = 2 * 1024L * 1024;

    /** Attribute paths of the item entity that can be used by the generic search API. */
    private static final Set<String> SEARCHABLE_FIELDS = Set.of(
            "code", "name", "description", "itemType", "unit", "active",
            "category.id", "price.price", "price.currency", "price.vatRate");

    private final ItemRepository itemRepository;
    private final ItemCategoryRepository itemCategoryRepository;
    private final OrderItemRepository orderItemRepository;
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

    @Transactional
    public ItemDto createItem(ItemRequest request) {
        ItemEntity entity = ItemEntity.builder()
                .code(request.code())
                .name(request.name())
                .description(request.description())
                .itemType(request.itemType())
                .unit(request.unit())
                .active(request.active())
                .category(resolveCategory(request.categoryId()))
                .build();
        applyPrice(entity, request);
        ItemEntity saved = itemRepository.save(entity);
        log.info("Created catalogue item {} ({}) with id {}", request.code(), request.name(), saved.getId());
        return itemMapper.toDto(saved);
    }

    @Transactional
    public ItemDto updateItem(UUID id, ItemRequest request) {
        ItemEntity entity = getItem(id);
        entity.setCode(request.code());
        entity.setName(request.name());
        entity.setDescription(request.description());
        entity.setItemType(request.itemType());
        entity.setUnit(request.unit());
        entity.setActive(request.active());
        entity.setCategory(resolveCategory(request.categoryId()));
        applyPrice(entity, request);
        log.info("Updating catalogue item {} ({})", id, request.code());
        return itemMapper.toDto(itemRepository.save(entity));
    }

    /**
     * Deletes the item. When the item is referenced by at least one order it is only deactivated,
     * so historical orders stay intact; the result tells the frontend which action was taken.
     */
    @Transactional
    public ItemDeleteResult deleteItem(UUID id) {
        ItemEntity entity = getItem(id);
        if (orderItemRepository.existsByItemId(id)) {
            entity.setActive(false);
            itemRepository.save(entity);
            log.info("Catalogue item {} ({}) is referenced by orders, deactivated instead of deleted", id, entity.getCode());
            return new ItemDeleteResult(false, true);
        }
        itemRepository.delete(entity);
        log.info("Deleted catalogue item {} ({})", id, entity.getCode());
        return new ItemDeleteResult(true, false);
    }

    /**
     * Stores the image of the item shown by the frontend in the catalogue. Only common image
     * media types are accepted and the file size is limited (see the constants above).
     */
    @Transactional
    public ItemDto uploadImage(UUID id, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Image file must not be empty");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Unsupported image content type " + contentType
                    + ". Allowed types are: " + String.join(", ", ALLOWED_IMAGE_CONTENT_TYPES));
        }
        if (file.getSize() > MAX_IMAGE_SIZE_BYTES) {
            throw new IllegalArgumentException("Image file exceeds the maximum allowed size of " + MAX_IMAGE_SIZE_BYTES + " bytes");
        }
        ItemEntity entity = getItem(id);
        try {
            entity.setImage(file.getBytes());
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read the uploaded image file", e);
        }
        entity.setImageContentType(contentType);
        log.info("Uploaded image ({}, {} bytes) for catalogue item {} ({})", contentType, file.getSize(), id, entity.getCode());
        return itemMapper.toDto(itemRepository.save(entity));
    }

    public ItemImageDto getImage(UUID id) {
        ItemEntity entity = getItem(id);
        if (entity.getImage() == null) {
            throw new NotFoundException("Item " + id + " has no image");
        }
        return new ItemImageDto(entity.getImage(), entity.getImageContentType());
    }

    @Transactional
    public ItemDto deleteImage(UUID id) {
        ItemEntity entity = getItem(id);
        entity.setImage(null);
        entity.setImageContentType(null);
        log.info("Deleted image of catalogue item {} ({})", id, entity.getCode());
        return itemMapper.toDto(itemRepository.save(entity));
    }

    @Transactional
    public ItemCategoryDto createCategory(ItemCategoryRequest request) {
        ItemCategoryEntity parent = resolveCategory(request.parentId());
        ItemCategoryEntity entity = ItemCategoryEntity.builder()
                .code(request.code())
                .name(request.name())
                .description(request.description())
                .parent(parent)
                .sortOrder(request.sortOrder())
                .active(request.active())
                .path(buildPath(parent, request.code()))
                .build();
        ItemCategoryEntity saved = itemCategoryRepository.save(entity);
        log.info("Created item category {} ({}) with id {}", request.code(), request.name(), saved.getId());
        return itemMapper.toDto(saved);
    }

    @Transactional
    public ItemCategoryDto updateCategory(UUID id, ItemCategoryRequest request) {
        ItemCategoryEntity entity = getCategory(id);
        ItemCategoryEntity parent = resolveCategory(request.parentId());
        if (parent != null && (parent.getId().equals(id) || parent.getPath().startsWith(entity.getPath() + "."))) {
            throw new IllegalStateException("A category cannot be moved under itself or its own subtree");
        }
        entity.setCode(request.code());
        entity.setName(request.name());
        entity.setDescription(request.description());
        entity.setParent(parent);
        entity.setSortOrder(request.sortOrder());
        entity.setActive(request.active());
        entity.setPath(buildPath(parent, request.code()));
        recomputeChildPaths(entity);
        log.info("Updating item category {} ({})", id, request.code());
        return itemMapper.toDto(itemCategoryRepository.save(entity));
    }

    /**
     * Deletes the category including its whole subtree. When the subtree still contains items and
     * {@code force} is false, nothing is deleted and the result asks the frontend for confirmation.
     * When forced, the assigned items are deleted, or only deactivated if referenced by orders.
     */
    @Transactional
    public ItemCategoryDeleteResult deleteCategory(UUID id, boolean force) {
        ItemCategoryEntity entity = getCategory(id);
        List<UUID> subtreeIds = itemCategoryRepository.findAllByPathStartingWith(entity.getPath()).stream()
                .filter(c -> c.getId().equals(id) || c.getPath().startsWith(entity.getPath() + "."))
                .map(ItemCategoryEntity::getId)
                .toList();
        List<ItemEntity> items = itemRepository.findAllByCategoryIdIn(subtreeIds);
        if (!items.isEmpty() && !force) {
            return new ItemCategoryDeleteResult(false, true, items.size(), 0, 0);
        }
        long deletedItems = 0;
        long deactivatedItems = 0;
        for (ItemEntity item : items) {
            if (orderItemRepository.existsByItemId(item.getId())) {
                item.setCategory(null);
                item.setActive(false);
                itemRepository.save(item);
                deactivatedItems++;
            } else {
                itemRepository.delete(item);
                deletedItems++;
            }
        }
        itemCategoryRepository.delete(entity);
        log.info("Deleted item category {} ({}) with {} items deleted and {} items deactivated",
                id, entity.getCode(), deletedItems, deactivatedItems);
        return new ItemCategoryDeleteResult(true, false, items.size(), deletedItems, deactivatedItems);
    }

    private ItemEntity getItem(UUID id) {
        return itemRepository.findById(id).orElseThrow(() -> new NotFoundException("Item", id));
    }

    private ItemCategoryEntity getCategory(UUID id) {
        return itemCategoryRepository.findById(id).orElseThrow(() -> new NotFoundException("ItemCategory", id));
    }

    private ItemCategoryEntity resolveCategory(UUID categoryId) {
        return categoryId == null ? null : getCategory(categoryId);
    }

    /** Creates or updates the current price record of the item from the request. */
    private void applyPrice(ItemEntity entity, ItemRequest request) {
        ItemPriceEntity price = entity.getPrice();
        if (price == null) {
            price = ItemPriceEntity.builder().item(entity).validFrom(LocalDateTime.now(ZoneId.systemDefault())).build();
            entity.setPrice(price);
        }
        price.setPrice(request.price());
        price.setCurrency(request.currency());
        price.setPurchasePriceNet(request.purchasePriceNet());
        price.setVatRate(request.vatRate());
    }

    /** Materialized path of hierarchical codes, e.g. "1.1.2" for "Paper / Hygienic / Tissues". */
    private String buildPath(ItemCategoryEntity parent, String code) {
        return parent == null ? code : parent.getPath() + "." + code;
    }

    /** Recursively recomputes materialized paths of the whole subtree after a move or code change. */
    private void recomputeChildPaths(ItemCategoryEntity entity) {
        for (ItemCategoryEntity child : entity.getChildren()) {
            child.setPath(buildPath(entity, child.getCode()));
            recomputeChildPaths(child);
        }
    }
}
