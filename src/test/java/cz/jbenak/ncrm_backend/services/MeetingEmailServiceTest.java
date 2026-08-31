package cz.jbenak.ncrm_backend.services;

import cz.jbenak.ncrm_backend.model.entity.company.SalesRepresentativeEntity;
import cz.jbenak.ncrm_backend.model.entity.customer.ContactPersonEntity;
import cz.jbenak.ncrm_backend.model.entity.customer.CustomerEntity;
import cz.jbenak.ncrm_backend.model.entity.customer.MeetingEntity;
import cz.jbenak.ncrm_backend.model.entity.security.UserEntity;
import jakarta.mail.BodyPart;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests of {@link MeetingEmailService}: recipient resolution, the .ics attachment
 * and error resilience.
 */
@ExtendWith(MockitoExtension.class)
class MeetingEmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private MeetingEmailService meetingEmailService;

    @BeforeEach
    void setUpSender() {
        ReflectionTestUtils.setField(meetingEmailService, "mailFromAddress", "noreply@ncrm.cz");
        ReflectionTestUtils.setField(meetingEmailService, "mailFromName", "nCRM");
    }

    @Test
    void sendMeetingCreatedSendsIcsToCustomerAndRepresentative() throws Exception {
        MeetingEntity meeting = meeting();
        MimeMessage message = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(message);

        meetingEmailService.sendMeetingCreated(meeting);

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        MimeMessage sent = captor.getValue();
        sent.saveChanges();
        assertThat(sent.getFrom()[0]).hasToString("nCRM <noreply@ncrm.cz>");
        assertThat(sent.getAllRecipients()).extracting(Object::toString)
                .containsExactly("customer@acme.cz", "rep@ncrm.cz");
        assertThat(sent.getSubject()).contains("pozvánka na schůzku").contains("Introduction");
        String ics = extractIcs(sent);
        assertThat(ics).contains("BEGIN:VCALENDAR")
                .contains("METHOD:REQUEST")
                .contains("UID:" + meeting.getId() + "@ncrm")
                .contains("SUMMARY:Introduction")
                .contains("SEQUENCE:0");
    }

    @Test
    void sendMeetingUpdatedPrefersContactPersonAndIncreasesSequence() throws Exception {
        MeetingEntity meeting = meeting();
        ContactPersonEntity contact = new ContactPersonEntity();
        contact.setEmail("contact@acme.cz");
        meeting.setContactPerson(contact);
        MimeMessage message = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(message);

        meetingEmailService.sendMeetingUpdated(meeting);

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        MimeMessage sent = captor.getValue();
        sent.saveChanges();
        assertThat(sent.getAllRecipients()).extracting(Object::toString)
                .containsExactly("contact@acme.cz", "rep@ncrm.cz");
        assertThat(sent.getSubject()).contains("změna schůzky");
        assertThat(extractIcs(sent)).contains("SEQUENCE:1");
    }

    @Test
    void representativeUserEmailIsUsedWhenBusinessEmailMissing() throws Exception {
        MeetingEntity meeting = meeting();
        meeting.getSalesRepresentative().setBusinessEmail(null);
        UserEntity user = new UserEntity();
        user.setEmail("user@ncrm.cz");
        meeting.getSalesRepresentative().setUser(user);
        MimeMessage message = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(message);

        meetingEmailService.sendMeetingCreated(meeting);

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        MimeMessage sent = captor.getValue();
        sent.saveChanges();
        assertThat(sent.getAllRecipients()).extracting(Object::toString)
                .containsExactly("customer@acme.cz", "user@ncrm.cz");
    }

    @Test
    void sendIsSkippedWhenNoEmailAddressIsAvailable() {
        MeetingEntity meeting = meeting();
        meeting.getCustomer().setEmail(null);
        meeting.getSalesRepresentative().setBusinessEmail(null);

        meetingEmailService.sendMeetingCreated(meeting);

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void sendFailureIsOnlyLoggedAndDoesNotPropagate() {
        MeetingEntity meeting = meeting();
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
        doThrow(new MailSendException("SMTP down")).when(mailSender).send(any(MimeMessage.class));

        assertThatCode(() -> meetingEmailService.sendMeetingCreated(meeting)).doesNotThrowAnyException();
    }

    /** Finds the .ics attachment in the sent message and returns its content. */
    private String extractIcs(MimeMessage message) throws Exception {
        MimeMultipart multipart = (MimeMultipart) message.getContent();
        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart part = multipart.getBodyPart(i);
            if ("schuzka.ics".equals(part.getFileName())) {
                return new String(part.getInputStream().readAllBytes());
            }
        }
        throw new AssertionError("The .ics attachment was not found in the message");
    }

    private MeetingEntity meeting() {
        CustomerEntity customer = new CustomerEntity();
        customer.setName("ACME");
        customer.setEmail("customer@acme.cz");

        SalesRepresentativeEntity representative = new SalesRepresentativeEntity();
        representative.setBusinessEmail("rep@ncrm.cz");

        MeetingEntity meeting = new MeetingEntity();
        meeting.setId(UUID.randomUUID());
        meeting.setCustomer(customer);
        meeting.setSalesRepresentative(representative);
        meeting.setSubject("Introduction");
        meeting.setDescription("First meeting");
        meeting.setPlannedDate(LocalDateTime.of(2026, Month.JULY, 20, 10, 0));
        meeting.setStatus(MeetingEntity.MeetingStatus.PLANNED);
        return meeting;
    }
}
