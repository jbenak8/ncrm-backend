package cz.jbenak.ncrm_backend.ai;

import cz.jbenak.ncrm_backend.model.dto.store.ItemDto;
import cz.jbenak.ncrm_backend.services.ItemService;
import cz.jbenak.ncrm_backend.services.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests of {@link CrmAiTools} covering the catalogue search filtering and the order lookups
 * exposed to the AI agents as tools.
 */
@ExtendWith(MockitoExtension.class)
class CrmAiToolsTest {

    @Mock
    private ItemService itemService;
    @Mock
    private OrderService orderService;
    @InjectMocks
    private CrmAiTools tools;

    private static ItemDto item(String code, String name, String description, String categoryName) {
        return new ItemDto(UUID.randomUUID(), code, name, description, null, null, categoryName,
                "ks", true, false, null);
    }

    @Test
    void searchCatalogueItemsFiltersByNameCodeDescriptionAndCategoryCaseInsensitive() {
        when(itemService.findAllActive()).thenReturn(List.of(
                item("TP-01", "Toaletní papír 3vrstvý", null, "Hygiena"),
                item("UT-01", "Utěrky", "Papírové utěrky", "Hygiena"),
                item("KP-01", "Kancelářský papír A4", null, "Papír"),
                item("MY-01", "Mýdlo", null, "Hygiena")));

        List<ItemDto> result = tools.searchCatalogueItems("PAPÍR");

        assertThat(result).extracting(ItemDto::code).containsExactly("TP-01", "UT-01", "KP-01");
    }

    @Test
    void searchCatalogueItemsWithBlankQueryReturnsEverything() {
        when(itemService.findAllActive()).thenReturn(List.of(item("A", "A", null, null)));

        assertThat(tools.searchCatalogueItems("  ")).hasSize(1);
    }

    @Test
    void listRecentOrdersClampsTheLimit() {
        when(orderService.search(anyList(), any(Pageable.class)))
                .thenAnswer(inv -> {
                    Pageable pageable = inv.getArgument(1);
                    assertThat(pageable.getPageSize()).isEqualTo(50);
                    return Page.empty(pageable);
                });

        assertThat(tools.listRecentOrders(500)).isEmpty();
    }

    @Test
    void searchOrdersByCustomerNameBuildsLikeFilter() {
        when(orderService.search(eq(List.of("customer.name:like:Novák")), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        assertThat(tools.searchOrdersByCustomerName("Novák")).isEmpty();
    }
}
