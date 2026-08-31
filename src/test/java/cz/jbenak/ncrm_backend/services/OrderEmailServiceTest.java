package cz.jbenak.ncrm_backend.services;

import cz.jbenak.ncrm_backend.model.entity.company.CompanyEntity;
import cz.jbenak.ncrm_backend.model.entity.customer.ContactPersonEntity;
import cz.jbenak.ncrm_backend.model.entity.customer.CustomerEntity;
import cz.jbenak.ncrm_backend.model.entity.order.OrderEntity;
import cz.jbenak.ncrm_backend.model.entity.order.OrderItemEntity;
import cz.jbenak.ncrm_backend.model.entity.store.ItemEntity;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests of {@link OrderEmailService}: recipient resolution, e-mail content and error resilience.
 */
@ExtendWith(MockitoExtension.class)
class OrderEmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private OrderEmailService orderEmailService;

    @BeforeEach
    void setUpSender() {
        ReflectionTestUtils.setField(orderEmailService, "mailFromAddress", "noreply@ncrm.cz");
        ReflectionTestUtils.setField(orderEmailService, "mailFromName", "nCRM");
    }

    @Test
    void sendOrderCreatedSendsSummaryToContactPerson() throws Exception {
        OrderEntity order = order();
        ContactPersonEntity contact = new ContactPersonEntity();
        contact.setEmail("contact@acme.cz");
        order.setContactPerson(contact);
        MimeMessage message = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(message);

        orderEmailService.sendOrderCreated(order);

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        MimeMessage sent = captor.getValue();
        assertThat(sent.getFrom()[0]).hasToString("nCRM <noreply@ncrm.cz>");
        assertThat(sent.getAllRecipients()[0]).hasToString("contact@acme.cz");
        assertThat(sent.getSubject()).contains("nová objednávka").contains("ORD-1");
        assertThat(sent.getContent().toString()).contains("Papír").contains("100.00").contains("CZK");
    }

    @Test
    void sendOrderCreatedFallsBackToCustomerEmail() throws Exception {
        OrderEntity order = order();
        MimeMessage message = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(message);

        orderEmailService.sendOrderCreated(order);

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        assertThat(captor.getValue().getAllRecipients()[0]).hasToString("customer@acme.cz");
    }

    @Test
    void sendIsSkippedWhenNoEmailAddressIsAvailable() {
        OrderEntity order = order();
        order.getCustomer().setEmail(null);

        orderEmailService.sendOrderCreated(order);

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void sendCustomerOrderReceivedGoesToCompanyEmail() throws Exception {
        OrderEntity order = order();
        CompanyEntity company = new CompanyEntity();
        company.setName("Moje firma");
        company.setEmail("orders@company.local");
        order.setCompany(company);
        MimeMessage message = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(message);

        orderEmailService.sendCustomerOrderReceived(order);

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        MimeMessage sent = captor.getValue();
        assertThat(sent.getAllRecipients()[0]).hasToString("orders@company.local");
        assertThat(sent.getSubject()).contains("zákaznická objednávka").contains("ORD-1");
    }

    @Test
    void sendCustomerOrderReceivedIsSkippedWithoutCompanyEmail() {
        OrderEntity order = order();

        orderEmailService.sendCustomerOrderReceived(order);

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void sendOrderStatusChangedContainsBothStatuses() throws Exception {
        OrderEntity order = order();
        order.setStatus(OrderEntity.OrderStatus.CONFIRMED);
        MimeMessage message = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(message);

        orderEmailService.sendOrderStatusChanged(order, OrderEntity.OrderStatus.NEW);

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        MimeMessage sent = captor.getValue();
        assertThat(sent.getSubject()).contains("změna stavu");
        assertThat(sent.getContent().toString()).contains("Nová").contains("Potvrzená");
    }

    @Test
    void sendFailureIsOnlyLoggedAndDoesNotPropagate() {
        OrderEntity order = order();
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
        doThrow(new MailSendException("SMTP down")).when(mailSender).send(any(MimeMessage.class));

        assertThatCode(() -> orderEmailService.sendOrderUpdated(order)).doesNotThrowAnyException();
    }

    private OrderEntity order() {
        CustomerEntity customer = new CustomerEntity();
        customer.setEmail("customer@acme.cz");

        ItemEntity item = new ItemEntity();
        item.setName("Papír");

        OrderItemEntity line = new OrderItemEntity();
        line.setItem(item);
        line.setQuantity(new BigDecimal("2"));
        line.setUnitPrice(new BigDecimal("50.00"));
        line.setTotalPrice(new BigDecimal("100.00"));

        OrderEntity order = new OrderEntity();
        order.setOrderNumber("ORD-1");
        order.setCustomer(customer);
        order.setOrderDate(LocalDate.of(2026, Month.JULY, 16));
        order.setStatus(OrderEntity.OrderStatus.NEW);
        order.setCurrency("CZK");
        order.setTotalPrice(new BigDecimal("100.00"));
        order.addItem(line);
        return order;
    }
}
