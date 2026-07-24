package cz.jbenak.ncrm_backend.controler;

import cz.jbenak.ncrm_backend.configuration.SecurityConfig;
import cz.jbenak.ncrm_backend.services.DashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests of {@link DashboardController} verifying that dashboards are restricted
 * to the owner, except the sales-by-representative widget which is also available to representatives.
 */
@WebMvcTest(DashboardController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("local")
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService dashboardService;

    @Test
    void anonymousIsRejected() throws Exception {
        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void ownerCanReadDashboard() throws Exception {
        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/dashboard/orders-by-month"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/dashboard/top-customers"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "SALES_REPRESENTATIVE")
    void salesRepresentativeCannotReadOwnerDashboard() throws Exception {
        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "SALES_REPRESENTATIVE")
    void salesRepresentativeCanReadOwnPerformance() throws Exception {
        when(dashboardService.salesByRepresentative()).thenReturn(List.of());
        mockMvc.perform(get("/api/dashboard/sales-by-representative"))
                .andExpect(status().isOk());
    }
}
