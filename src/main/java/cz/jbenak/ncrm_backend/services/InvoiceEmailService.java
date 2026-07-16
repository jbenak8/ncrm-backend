package cz.jbenak.ncrm_backend.services;

import cz.jbenak.ncrm_backend.model.entity.invoice.InvoiceEntity;
import cz.jbenak.ncrm_backend.model.entity.order.OrderEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-16
 * Sends the issued invoice to the customer by e-mail with the printable PDF attached.
 * The recipient is the order contact person, or the customer's own e-mail address when
 * no contact person (or their e-mail) is available. Unlike the order notifications, a failure
 * to send the invoice is propagated to the caller, because the invoice dispatch is requested
 * explicitly by the user.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceEmailService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("d.M.yyyy");
    public static final String BR = "<br/>";

    private final JavaMailSender mailSender;

    /** Sends the invoice with the PDF attachment to the customer. */
    public void sendInvoice(InvoiceEntity invoice, byte[] pdf) {
        String recipient = resolveRecipient(invoice.getOrder());
        if (recipient == null) {
            throw new IllegalStateException("Invoice " + invoice.getInvoiceNumber()
                    + " has no customer e-mail address, it cannot be sent");
        }
        try {
            var message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(recipient);
            helper.setSubject("nCRM – faktura " + invoice.getInvoiceNumber());
            helper.setText(buildBody(invoice), true);
            helper.addAttachment("faktura-" + invoice.getInvoiceNumber() + ".pdf",
                    new ByteArrayResource(pdf), "application/pdf");
            mailSender.send(message);
            log.info("Invoice {} sent to {}", invoice.getInvoiceNumber(), recipient);
        } catch (Exception e) {
            log.error("Failed to send invoice {} to {}", invoice.getInvoiceNumber(), recipient, e);
            throw new IllegalStateException("Failed to send invoice " + invoice.getInvoiceNumber()
                    + ": " + e.getMessage(), e);
        }
    }

    /** Recipient of the invoice: the contact person's e-mail, or the customer's e-mail as a fallback. */
    private String resolveRecipient(OrderEntity order) {
        if (order == null) {
            return null;
        }
        if (order.getContactPerson() != null && isNotBlank(order.getContactPerson().getEmail())) {
            return order.getContactPerson().getEmail();
        }
        if (order.getCustomer() != null && isNotBlank(order.getCustomer().getEmail())) {
            return order.getCustomer().getEmail();
        }
        return null;
    }

    /** Builds the HTML body of the invoice e-mail (header data, payment details and total). */
    private String buildBody(InvoiceEntity invoice) {
        StringBuilder html = new StringBuilder();
        html.append("<p>Dobrý den,</p><p>v příloze zasíláme fakturu <strong>")
                .append(invoice.getInvoiceNumber()).append("</strong>");
        if (invoice.getOrder() != null) {
            html.append(" k objednávce <strong>").append(invoice.getOrder().getOrderNumber()).append("</strong>");
        }
        html.append(".</p>");
        html.append("<p>Forma úhrady: ")
                .append(invoice.getPaymentType() == InvoiceEntity.PaymentType.CASH ? "hotově" : "převodem")
                .append(BR);
        if (invoice.getDueDate() != null) {
            html.append("Datum splatnosti: ").append(invoice.getDueDate().format(DATE_FORMAT)).append(BR);
        }
        if (invoice.getPaymentType() == InvoiceEntity.PaymentType.TRANSFER
                && isNotBlank(invoice.getVariableSymbol())) {
            html.append("Variabilní symbol: ").append(invoice.getVariableSymbol()).append(BR);
        }
        html.append("Celkem k úhradě: <strong>").append(invoice.getTotalGross());
        if (invoice.getCurrency() != null) {
            html.append(" ").append(invoice.getCurrency());
        }
        html.append("</strong></p>");
        return html.toString();
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }
}
