package cz.jbenak.ncrm_backend.ai;

import cz.jbenak.ncrm_backend.model.dto.order.OrderDto;
import cz.jbenak.ncrm_backend.model.dto.store.ItemCategoryDto;
import cz.jbenak.ncrm_backend.model.dto.store.ItemDto;
import cz.jbenak.ncrm_backend.services.ItemService;
import cz.jbenak.ncrm_backend.services.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-08-26
 * Read-only tools (function calling) exposed to the AI agents so that they can work with real CRM
 * data — the store catalogue (items and their prices), the item category tree and customer orders.
 * Used by {@link AiService} for the conversational chat and for campaign content generation, e.g.
 * "Create a campaign for toilet paper with a 5% discount" resolves the real item and its price.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CrmAiTools {

    private static final int MAX_RESULTS = 50;

    private final ItemService itemService;
    private final OrderService orderService;

    @Tool(description = "Returns all active items (goods and services) of the store catalogue "
            + "including their code, name, description, unit, category and current price with currency and VAT rate.")
    public List<ItemDto> listCatalogueItems() {
        log.info("AI tool call: listCatalogueItems");
        return itemService.findAllActive();
    }

    @Tool(description = "Searches active catalogue items whose name, code or description contains the given text "
            + "(case-insensitive). Returns the matching items including their current price with currency and VAT rate.")
    public List<ItemDto> searchCatalogueItems(
            @ToolParam(description = "Text to search for in the item name, code or description, e.g. 'toaletní papír'.")
            String query) {
        log.info("AI tool call: searchCatalogueItems('{}')", query);
        String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return itemService.findAllActive().stream()
                .filter(item -> needle.isEmpty()
                        || contains(item.name(), needle)
                        || contains(item.code(), needle)
                        || contains(item.description(), needle)
                        || contains(item.categoryName(), needle))
                .limit(MAX_RESULTS)
                .toList();
    }

    @Tool(description = "Returns the hierarchical tree of item categories (groups of the store catalogue) "
            + "including their codes, names and descriptions.")
    public List<ItemCategoryDto> listItemCategories() {
        log.info("AI tool call: listItemCategories");
        return itemService.findCategoryTree();
    }

    @Tool(description = "Returns all catalogue items belonging to the item category (group) with the given id.")
    public List<ItemDto> listItemsInCategory(
            @ToolParam(description = "UUID of the item category, e.g. from the listItemCategories tool.")
            String categoryId) {
        log.info("AI tool call: listItemsInCategory({})", categoryId);
        return itemService.findByCategory(UUID.fromString(categoryId));
    }

    @Tool(description = "Returns the most recent customer orders (newest first) including their items, "
            + "quantities, prices and status. Useful to see what customers actually buy.")
    public List<OrderDto> listRecentOrders(
            @ToolParam(description = "Maximum number of orders to return (1-50).", required = false)
            Integer limit) {
        log.info("AI tool call: listRecentOrders({})", limit);
        int size = limit == null ? 20 : Math.clamp(limit, 1, MAX_RESULTS);
        return orderService.search(List.of(),
                        PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "orderDate")))
                .getContent();
    }

    @Tool(description = "Returns orders whose customer name contains the given text (case-insensitive), "
            + "including their items, quantities, prices and status.")
    public List<OrderDto> searchOrdersByCustomerName(
            @ToolParam(description = "Part of the customer name, e.g. 'Novák'.")
            String customerName) {
        log.info("AI tool call: searchOrdersByCustomerName('{}')", customerName);
        return orderService.search(List.of("customer.name:like:" + customerName),
                        PageRequest.of(0, MAX_RESULTS, Sort.by(Sort.Direction.DESC, "orderDate")))
                .getContent();
    }

    private boolean contains(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
    }
}
