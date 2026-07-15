package cz.jbenak.ncrm_backend.controler;

import cz.jbenak.ncrm_backend.model.dto.store.ItemCategoryDeleteResult;
import cz.jbenak.ncrm_backend.model.dto.store.ItemCategoryDto;
import cz.jbenak.ncrm_backend.model.dto.store.ItemCategoryRequest;
import cz.jbenak.ncrm_backend.model.dto.store.ItemDeleteResult;
import cz.jbenak.ncrm_backend.model.dto.store.ItemDto;
import cz.jbenak.ncrm_backend.model.dto.store.ItemRequest;
import cz.jbenak.ncrm_backend.services.ItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-11
 * REST API for the store catalogue (items and the category tree) used when creating orders.
 * Reading is available to all authenticated roles, modifications are restricted to the owner/admin.
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

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ItemDto create(@Valid @RequestBody ItemRequest request) {
        return itemService.createItem(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ItemDto update(@PathVariable UUID id, @Valid @RequestBody ItemRequest request) {
        return itemService.updateItem(id, request);
    }

    /**
     * Deletes the item; when it is referenced by an order it is only deactivated
     * and the returned result reports which action was taken.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ItemDeleteResult delete(@PathVariable UUID id) {
        return itemService.deleteItem(id);
    }

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ItemCategoryDto createCategory(@Valid @RequestBody ItemCategoryRequest request) {
        return itemService.createCategory(request);
    }

    @PutMapping("/categories/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ItemCategoryDto updateCategory(@PathVariable UUID id, @Valid @RequestBody ItemCategoryRequest request) {
        return itemService.updateCategory(id, request);
    }

    /**
     * Deletes the category with its subtree. Without {@code force} the request is rejected when the
     * category still contains items ({@code requiresConfirmation} is returned), so the frontend can
     * warn the user; with {@code force=true} the items are deleted or deactivated (see the service).
     */
    @DeleteMapping("/categories/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ItemCategoryDeleteResult deleteCategory(@PathVariable UUID id,
                                                   @RequestParam(defaultValue = "false") boolean force) {
        return itemService.deleteCategory(id, force);
    }
}
