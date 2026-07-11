package cz.jbenak.ncrm_backend.controler;

import cz.jbenak.ncrm_backend.model.dto.store.ItemCategoryDto;
import cz.jbenak.ncrm_backend.model.dto.store.ItemDto;
import cz.jbenak.ncrm_backend.services.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
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
@PreAuthorize("hasAnyRole('OWNER', 'SALES_REPRESENTATIVE', 'CUSTOMER')")
public class ItemController {

    private final ItemService itemService;

    @GetMapping
    public List<ItemDto> findAllActive() {
        return itemService.findAllActive();
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
