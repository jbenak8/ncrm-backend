package cz.jbenak.ncrm_backend.controler;

import cz.jbenak.ncrm_backend.configuration.SecurityConfig;
import cz.jbenak.ncrm_backend.services.CustomerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-11
 * Integration tests of {@link CustomerController} verifying the role-based access rules
 * (owner vs. sales representative vs. customer) using the "local" security profile.
 */
@WebMvcTest(CustomerController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("local")
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomerService customerService;

    @Test
    void anonymousIsRejected() throws Exception {
        mockMvc.perform(get("/api/customers"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "SALES_REPRESENTATIVE")
    void salesRepresentativeCanListCustomers() throws Exception {
        when(customerService.findAll(any(), any(Pageable.class))).thenReturn(Page.empty());
        mockMvc.perform(get("/api/customers"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void customerCannotListCustomers() throws Exception {
        mockMvc.perform(get("/api/customers"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "SALES_REPRESENTATIVE")
    void salesRepresentativeCannotDeactivateCustomer() throws Exception {
        mockMvc.perform(delete("/api/customers/{id}", UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void ownerCanDeactivateCustomer() throws Exception {
        mockMvc.perform(delete("/api/customers/{id}", UUID.randomUUID()))
                .andExpect(status().isNoContent());
    }
}
