package cz.jbenak.ncrm_backend.controler;

import cz.jbenak.ncrm_backend.configuration.SecurityConfig;
import cz.jbenak.ncrm_backend.model.dto.company.CompanyLogoDto;
import cz.jbenak.ncrm_backend.services.CompanyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests of {@link CompanyController} verifying that the administration of own
 * companies is restricted to the owner.
 */
@WebMvcTest(CompanyController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("local")
class CompanyControllerTest {

    private static final String VALID_COMPANY = """
            {
              "name": "ACME s.r.o.",
              "registrationId": "12345678",
              "active": true,
              "defaultCompany": false
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CompanyService companyService;

    @Test
    void anonymousIsRejected() throws Exception {
        mockMvc.perform(get("/api/companies"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "owner", roles = "OWNER")
    void ownerCanListCompanies() throws Exception {
        when(companyService.findAllForUser("owner", false)).thenReturn(List.of());
        mockMvc.perform(get("/api/companies"))
                .andExpect(status().isOk());
        verify(companyService).findAllForUser("owner", false);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminListsAllCompanies() throws Exception {
        when(companyService.findAllForUser("admin", true)).thenReturn(List.of());
        mockMvc.perform(get("/api/companies"))
                .andExpect(status().isOk());
        verify(companyService).findAllForUser("admin", true);
    }

    // Sales representatives may list the companies (scoped to their assignment) so that
    // the frontend can filter the dashboard data by the active company.
    @Test
    @WithMockUser(username = "rep", roles = "SALES_REPRESENTATIVE")
    void salesRepresentativeCanListAssignedCompanies() throws Exception {
        when(companyService.findAllForUser("rep", false)).thenReturn(List.of());
        mockMvc.perform(get("/api/companies"))
                .andExpect(status().isOk());
        verify(companyService).findAllForUser("rep", false);
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void customerCannotListCompanies() throws Exception {
        mockMvc.perform(get("/api/companies"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void ownerCanCreateCompany() throws Exception {
        mockMvc.perform(post("/api/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_COMPANY))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void createIsRejectedWithoutMandatoryFields() throws Exception {
        mockMvc.perform(post("/api/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "owner", roles = "OWNER")
    void ownerCanDeleteCompanyWithAuditTrail() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(delete("/api/companies/{id}", id))
                .andExpect(status().isNoContent());
        verify(companyService).delete(id, "owner");
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void customerCannotSetDefaultCompany() throws Exception {
        mockMvc.perform(post("/api/companies/{id}/default", UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void ownerCanSetDefaultCompany() throws Exception {
        when(companyService.setDefault(any(UUID.class))).thenReturn(null);
        mockMvc.perform(post("/api/companies/{id}/default", UUID.randomUUID()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void ownerCanUploadLogo() throws Exception {
        UUID id = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "logo.png", MediaType.IMAGE_PNG_VALUE, new byte[]{1, 2, 3});
        mockMvc.perform(multipart(HttpMethod.PUT, "/api/companies/{id}/logo", id).file(file))
                .andExpect(status().isOk());
        verify(companyService).uploadLogo(eq(id), any());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void customerCannotUploadLogo() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "logo.png", MediaType.IMAGE_PNG_VALUE, new byte[]{1, 2, 3});
        mockMvc.perform(multipart(HttpMethod.PUT, "/api/companies/{id}/logo", UUID.randomUUID()).file(file))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void ownerCanDownloadLogo() throws Exception {
        UUID id = UUID.randomUUID();
        when(companyService.getLogo(id)).thenReturn(new CompanyLogoDto(new byte[]{1, 2, 3}, MediaType.IMAGE_PNG_VALUE));
        mockMvc.perform(get("/api/companies/{id}/logo", id))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(new byte[]{1, 2, 3}));
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void ownerCanDeleteLogo() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(delete("/api/companies/{id}/logo", id))
                .andExpect(status().isNoContent());
        verify(companyService).deleteLogo(id);
    }
}
