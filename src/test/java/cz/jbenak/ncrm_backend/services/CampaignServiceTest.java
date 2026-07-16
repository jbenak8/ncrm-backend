package cz.jbenak.ncrm_backend.services;

import cz.jbenak.ncrm_backend.ai.AiService;
import cz.jbenak.ncrm_backend.model.dto.ai.AiDtos;
import cz.jbenak.ncrm_backend.model.dto.marketing.CampaignRequest;
import cz.jbenak.ncrm_backend.model.entity.customer.CustomerEntity;
import cz.jbenak.ncrm_backend.model.entity.marketing.CampaignEntity;
import cz.jbenak.ncrm_backend.model.entity.marketing.CampaignRecipientEntity;
import cz.jbenak.ncrm_backend.model.mapper.CampaignMapper;
import cz.jbenak.ncrm_backend.repository.CampaignRepository;
import cz.jbenak.ncrm_backend.repository.CustomerRepository;
import cz.jbenak.ncrm_backend.repository.UserRepository;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests of {@link CampaignService} covering campaign creation with recipient resolution,
 * content extraction from uploaded files, e-mail sending with delivery statuses and cancellation.
 */
@ExtendWith(MockitoExtension.class)
class CampaignServiceTest {

    @Mock
    private CampaignRepository campaignRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CampaignMapper campaignMapper;
    @Mock
    private AiService aiService;
    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private CampaignService campaignService;

    private CustomerEntity customer(String email) {
        CustomerEntity customer = new CustomerEntity();
        customer.setName("ACME");
        customer.setEmail(email);
        return customer;
    }

    @Test
    void createSnapshotsRecipientEmailsAndSkipsCustomersWithoutEmail() {
        UUID withEmail = UUID.randomUUID();
        UUID withoutEmail = UUID.randomUUID();
        when(customerRepository.findById(withEmail)).thenReturn(Optional.of(customer("info@acme.cz")));
        when(customerRepository.findById(withoutEmail)).thenReturn(Optional.of(customer(null)));
        when(campaignRepository.save(any(CampaignEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        campaignService.create(new CampaignRequest("Summer", "Sale", "<p>Hi</p>",
                null, null, List.of(withEmail, withoutEmail)), null);

        ArgumentCaptor<CampaignEntity> captor = ArgumentCaptor.forClass(CampaignEntity.class);
        verify(campaignRepository).save(captor.capture());
        CampaignEntity saved = captor.getValue();
        assertThat(saved.getRecipients()).hasSize(1);
        assertThat(saved.getRecipients().getFirst().getEmail()).isEqualTo("info@acme.cz");
        assertThat(saved.getContentSource()).isEqualTo(CampaignEntity.ContentSource.MANUAL);
    }

    @Test
    void createFailsWithoutAnyValidRecipient() {
        UUID customerId = UUID.randomUUID();
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer(" ")));

        CampaignRequest request = new CampaignRequest("Summer", "Sale", "body",
                null, null, List.of(customerId));
        assertThatThrownBy(() -> campaignService.create(request, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no recipients");
    }

    @Test
    void createThrowsWhenCustomerMissing() {
        UUID customerId = UUID.randomUUID();
        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

        CampaignRequest request = new CampaignRequest("Summer", "Sale", "body",
                null, null, List.of(customerId));
        assertThatThrownBy(() -> campaignService.create(request, "owner"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void generateContentDelegatesToAiService() {
        AiDtos.ContentGenerationRequest request = new AiDtos.ContentGenerationRequest("Sale", null, null, null, null);

        campaignService.generateContent(request);

        verify(aiService).generateContent(request);
    }

    @Test
    void extractContentFromTextFileReturnsFileContent() {
        MockMultipartFile file = new MockMultipartFile("file", "campaign.html", "text/html",
                "<h1>Sale</h1>".getBytes());

        assertThat(campaignService.extractContentFromFile(file)).isEqualTo("<h1>Sale</h1>");
    }

    @Test
    void extractContentHandlesMissingFilename() {
        MockMultipartFile file = new MockMultipartFile("file", null, "text/plain", "hello".getBytes());

        assertThat(campaignService.extractContentFromFile(file)).isEqualTo("hello");
    }

    @Test
    void sendDeliversEmailsAndMarksCampaignSent() {
        UUID id = UUID.randomUUID();
        CampaignEntity campaign = CampaignEntity.builder()
                .subject("Sale").body("<p>Hi</p>").build();
        campaign.addRecipient(CampaignRecipientEntity.builder().email("a@acme.cz")
                .deliveryStatus(CampaignRecipientEntity.DeliveryStatus.PENDING).build());
        when(campaignRepository.findById(id)).thenReturn(Optional.of(campaign));
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
        when(campaignRepository.save(campaign)).thenReturn(campaign);

        campaignService.send(id);

        assertThat(campaign.getStatus()).isEqualTo(CampaignEntity.CampaignStatus.SENT);
        assertThat(campaign.getSentAt()).isNotNull();
        assertThat(campaign.getRecipients().getFirst().getDeliveryStatus())
                .isEqualTo(CampaignRecipientEntity.DeliveryStatus.SENT);
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendMarksFailedRecipientsAndKeepsError() {
        UUID id = UUID.randomUUID();
        CampaignEntity campaign = CampaignEntity.builder()
                .subject("Sale").body("<p>Hi</p>").build();
        campaign.addRecipient(CampaignRecipientEntity.builder().email("a@acme.cz")
                .deliveryStatus(CampaignRecipientEntity.DeliveryStatus.PENDING).build());
        when(campaignRepository.findById(id)).thenReturn(Optional.of(campaign));
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
        doThrow(new MailSendException("SMTP down")).when(mailSender).send(any(MimeMessage.class));
        when(campaignRepository.save(campaign)).thenReturn(campaign);

        campaignService.send(id);

        CampaignRecipientEntity recipient = campaign.getRecipients().getFirst();
        assertThat(recipient.getDeliveryStatus()).isEqualTo(CampaignRecipientEntity.DeliveryStatus.FAILED);
        assertThat(recipient.getErrorMessage()).contains("SMTP down");
    }

    @Test
    void sendSkipsAlreadyDeliveredRecipients() {
        UUID id = UUID.randomUUID();
        CampaignEntity campaign = CampaignEntity.builder()
                .subject("Sale").body("<p>Hi</p>").build();
        campaign.addRecipient(CampaignRecipientEntity.builder().email("a@acme.cz")
                .deliveryStatus(CampaignRecipientEntity.DeliveryStatus.SENT).build());
        when(campaignRepository.findById(id)).thenReturn(Optional.of(campaign));
        when(campaignRepository.save(campaign)).thenReturn(campaign);

        campaignService.send(id);

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void sendRejectsAlreadySentCampaign() {
        UUID id = UUID.randomUUID();
        CampaignEntity campaign = CampaignEntity.builder()
                .status(CampaignEntity.CampaignStatus.SENT).build();
        when(campaignRepository.findById(id)).thenReturn(Optional.of(campaign));

        assertThatThrownBy(() -> campaignService.send(id))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already been sent");
    }

    @Test
    void cancelSetsCancelledStatus() {
        UUID id = UUID.randomUUID();
        CampaignEntity campaign = new CampaignEntity();
        when(campaignRepository.findById(id)).thenReturn(Optional.of(campaign));

        campaignService.cancel(id);

        assertThat(campaign.getStatus()).isEqualTo(CampaignEntity.CampaignStatus.CANCELLED);
        verify(campaignRepository).save(campaign);
    }

    @Test
    void findByIdThrowsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(campaignRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> campaignService.findById(id))
                .isInstanceOf(NotFoundException.class);
    }
}
