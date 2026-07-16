package cz.jbenak.ncrm_backend.services;

import cz.jbenak.ncrm_backend.model.entity.customer.ContactPersonEntity;
import cz.jbenak.ncrm_backend.model.entity.customer.CustomerEntity;
import cz.jbenak.ncrm_backend.model.entity.invoice.InvoiceEntity;
import cz.jbenak.ncrm_backend.model.entity.order.OrderEntity;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

/**
 * Unit tests of {@link InvoiceEmailService}: recipient resolution, e-mail content
 * with the PDF attachment and error propagation to the caller.
 */
@ExtendWith(MockitoExtension.class)
class InvoiceEmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private InvoiceEmailService invoiceEmailService;

    @Test
    void sendInvoiceSendsPdfToContactPerson() throws Exception {
        InvoiceEntity invoice = invoice();
        ContactPersonEntity contact = new ContactPersonEntity();
        contact.setEmail("contact@acme.cz");
        invoice.getOrder().setContactPerson(contact);
        MimeMessage message = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(message);

        invoiceEmailService.sendInvoice(invoice, new byte[]{1, 2, 3});

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        MimeMessage sent = captor.getValue();
        assertThat(sent.getAllRecipients()[0]).hasToString("contact@acme.cz");
        assertThat(sent.getSubject()).contains("faktura").contains("2026-000042");
    }

    @Test
    void sendInvoiceFallsBackToCustomerEmail() throws Exception {
        InvoiceEntity invoice = invoice();
        MimeMessage message = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(message);

        invoiceEmailService.sendInvoice(invoice, new byte[]{1});

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        assertThat(captor.getValue().getAllRecipients()[0]).hasToString("customer@acme.cz");
    }

    @Test
    void sendInvoiceBodyContainsPaymentDetailsForTransfer() throws Exception {
        InvoiceEntity invoice = invoice();
        MimeMessage message = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(message);

        invoiceEmailService.sendInvoice(invoice, new byte[]{1});

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        String content = extractHtml(captor.getValue());
        assertThat(content).contains("2026-000042")
                .contains("ORD-1")
                .contains("2026000042")
                .contains("121.00")
                .contains("CZK");
    }

    @Test
    void sendInvoiceBodyForCashOmitsVariableSymbol() throws Exception {
        InvoiceEntity invoice = invoice();
        invoice.setPaymentType(InvoiceEntity.PaymentType.CASH);
        MimeMessage message = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(message);

        invoiceEmailService.sendInvoice(invoice, new byte[]{1});

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        String content = extractHtml(captor.getValue());
        assertThat(content).doesNotContain("2026000042");
    }

    @Test
    void sendInvoiceFailsWhenNoRecipientIsAvailable() {
        InvoiceEntity invoice = invoice();
        invoice.getOrder().getCustomer().setEmail(null);

        assertThatThrownBy(() -> invoiceEmailService.sendInvoice(invoice, new byte[]{1}))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no customer e-mail");
    }

    @Test
    void sendInvoiceFailsWhenOrderIsMissing() {
        InvoiceEntity invoice = invoice();
        invoice.setOrder(null);

        assertThatThrownBy(() -> invoiceEmailService.sendInvoice(invoice, new byte[]{1}))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no customer e-mail");
    }

    @Test
    void sendInvoicePropagatesMailFailure() {
        InvoiceEntity invoice = invoice();
        MimeMessage message = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(message);
        doThrow(new MailSendException("SMTP down")).when(mailSender).send(any(MimeMessage.class));

        assertThatThrownBy(() -> invoiceEmailService.sendInvoice(invoice, new byte[]{1}))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to send invoice");
    }

    private InvoiceEntity invoice() {
        CustomerEntity customer = new CustomerEntity();
        customer.setEmail("customer@acme.cz");
        OrderEntity order = new OrderEntity();
        order.setOrderNumber("ORD-1");
        order.setCustomer(customer);
        InvoiceEntity invoice = new InvoiceEntity();
        invoice.setInvoiceNumber("2026-000042");
        invoice.setOrder(order);
        invoice.setPaymentType(InvoiceEntity.PaymentType.TRANSFER);
        invoice.setDueDate(LocalDate.of(2026, Month.AUGUST, 1));
        invoice.setVariableSymbol("2026000042");
        invoice.setTotalGross(new BigDecimal("121.00"));
        invoice.setCurrency("CZK");
        return invoice;
    }

    private String extractHtml(MimeMessage message) throws Exception {
        Object content = message.getContent();
        if (content instanceof jakarta.mail.Multipart multipart) {
            return partText(multipart);
        }
        return content.toString();
    }

    private String partText(jakarta.mail.Multipart multipart) throws Exception {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < multipart.getCount(); i++) {
            Object part = multipart.getBodyPart(i).getContent();
            if (part instanceof jakarta.mail.Multipart nested) {
                text.append(partText(nested));
            } else if (part instanceof String string) {
                text.append(string);
            }
        }
        return text.toString();
    }
}
