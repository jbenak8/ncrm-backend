package cz.jbenak.ncrm_backend.controler;

import cz.jbenak.ncrm_backend.model.dto.store.ItemCategoryDto;
import cz.jbenak.ncrm_backend.model.dto.store.ItemDto;
import cz.jbenak.ncrm_backend.services.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-11
 * REST API for the store catalogue (items and the category tree) used when creating orders.
 * Read-only for all authenticated roles.
 */
@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'SALES_REPRESENTATIVE', 'CUSTOMER')")
public class ItemController {

    private final ItemService itemService;

    @GetMapping
    public List<ItemDto> findAllActive() {
        return itemService.findAllActive();
    }

    /**
     * Generic search endpoint. Accepts repeatable {@code filter} query parameters in the form
     * {@code field:operator:value} (operators: contains, notContains, eq, neq, lt, gt, between;
     * for between the value is {@code lower,upper}). All filters are combined with AND.
     * Example: {@code /api/items/search?filter=name:contains:paper&filter=price.price:between:10,100}
     */
    @GetMapping("/search")
    public Page<ItemDto> search(@RequestParam(required = false) List<String> filter, Pageable pageable) {
        return itemService.search(filter, pageable);
    }

    @GetMapping("/{id}")
    public ItemDto findById(@PathVariable UUID id) {
        return itemService.findById(id);
    }

    @GetMapping("/by-category/{categoryId}")
    public List<ItemDto> findByCategory(@PathVariable UUID categoryId) {
        return itemService.findByCategory(categoryId);
    }

    @GetMapping("/categories")
    public List<ItemCategoryDto> findCategoryTree() {
        return itemService.findCategoryTree();
    }
}
