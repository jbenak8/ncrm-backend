package cz.jbenak.ncrm_backend.controler;

import cz.jbenak.ncrm_backend.configuration.SecurityConfig;
import cz.jbenak.ncrm_backend.services.ItemService;
import cz.jbenak.ncrm_backend.services.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests of {@link ItemController} verifying that the store catalogue is readable
 * by all authenticated roles and unknown items are reported as 404 problem details.
 */
@WebMvcTest(ItemController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("local")
class ItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ItemService itemService;

    @Test
    void anonymousIsRejected() throws Exception {
        mockMvc.perform(get("/api/items"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void customerCanBrowseCatalogue() throws Exception {
        when(itemService.findAllActive()).thenReturn(List.of());
        when(itemService.findCategoryTree()).thenReturn(List.of());

        mockMvc.perform(get("/api/items"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/items/categories"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "SALES_REPRESENTATIVE")
    void unknownItemIsTranslatedToNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(itemService.findById(id)).thenThrow(new NotFoundException("Item", id));

        mockMvc.perform(get("/api/items/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void ownerCanBrowseByCategory() throws Exception {
        UUID categoryId = UUID.randomUUID();
        when(itemService.findByCategory(categoryId)).thenReturn(List.of());

        mockMvc.perform(get("/api/items/by-category/{categoryId}", categoryId))
                .andExpect(status().isOk());
    }
}
