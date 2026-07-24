package cz.jbenak.ncrm_backend.controler;

import cz.jbenak.ncrm_backend.configuration.SecurityConfig;
import cz.jbenak.ncrm_backend.model.entity.order.OrderEntity;
import cz.jbenak.ncrm_backend.security.CustomerScope;
import cz.jbenak.ncrm_backend.services.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests of {@link OrderController} verifying the role-based access rules:
 * internal roles manage orders, customers may only read orders.
 */
@WebMvcTest(OrderController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("local")
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private CustomerScope customerScope;

    @Test
    void anonymousIsRejected() throws Exception {
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "SALES_REPRESENTATIVE")
    void salesRepresentativeCanListOrders() throws Exception {
        when(orderService.findAll()).thenReturn(List.of());
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void customerListingOrdersIsScopedToOwnCustomer() throws Exception {
        UUID customerId = UUID.randomUUID();
        when(customerScope.isCustomer(any())).thenReturn(true);
        when(customerScope.customerId(any())).thenReturn(Optional.of(customerId));
        when(orderService.findByCustomer(customerId)).thenReturn(List.of());
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void customerCanListOwnOrders() throws Exception {
        UUID customerId = UUID.randomUUID();
        when(orderService.findByCustomer(customerId)).thenReturn(List.of());
        mockMvc.perform(get("/api/orders/by-customer/{customerId}", customerId))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void customerCanCreateOrderWithoutSalesRepresentative() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerId":"%s","orderDate":"2026-07-24",
                                 "items":[{"itemId":"%s","quantity":1}]}
                                """.formatted(UUID.randomUUID(), UUID.randomUUID())))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void customerCannotChangeOrderStatus() throws Exception {
        mockMvc.perform(post("/api/orders/{id}/status/{status}", UUID.randomUUID(),
                        OrderEntity.OrderStatus.CONFIRMED))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void ownerCanChangeOrderStatus() throws Exception {
        mockMvc.perform(post("/api/orders/{id}/status/{status}", UUID.randomUUID(),
                        OrderEntity.OrderStatus.CONFIRMED))
                .andExpect(status().isOk());
    }
}
