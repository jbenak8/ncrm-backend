package cz.jbenak.ncrm_backend.controler;

import cz.jbenak.ncrm_backend.configuration.SecurityConfig;
import cz.jbenak.ncrm_backend.services.ReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests of {@link ReportController} verifying the role restrictions of the PDF reports
 * and the PDF response headers.
 */
@WebMvcTest(ReportController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("local")
class ReportControllerTest {

    private static final byte[] PDF = "%PDF-1.7".getBytes();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReportService reportService;

    @Test
    void anonymousIsRejected() throws Exception {
        mockMvc.perform(get("/api/reports/owner/sales-overview"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void ownerReceivesSalesOverviewPdf() throws Exception {
        when(reportService.ownerSalesReport()).thenReturn(PDF);

        mockMvc.perform(get("/api/reports/owner/sales-overview"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"sales-overview.pdf\""));
    }

    @Test
    @WithMockUser(roles = "SALES_REPRESENTATIVE")
    void salesRepresentativeCannotReadOwnerReport() throws Exception {
        mockMvc.perform(get("/api/reports/owner/sales-overview"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "SALES_REPRESENTATIVE")
    void salesRepresentativeReceivesPerformancePdf() throws Exception {
        when(reportService.salesRepresentativePerformanceReport()).thenReturn(PDF);

        mockMvc.perform(get("/api/reports/representative/performance"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void customerReceivesOwnOrdersPdf() throws Exception {
        when(reportService.customerOrdersReport(any(UUID.class))).thenReturn(PDF);

        mockMvc.perform(get("/api/reports/customer/{customerId}/orders", UUID.randomUUID()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void customerCannotReadPerformanceReport() throws Exception {
        mockMvc.perform(get("/api/reports/representative/performance"))
                .andExpect(status().isForbidden());
    }
}
