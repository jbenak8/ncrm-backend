package cz.jbenak.ncrm_backend.services;

import cz.jbenak.ncrm_backend.model.entity.quotation.QuotationEntity;
import cz.jbenak.ncrm_backend.model.entity.quotation.QuotationItemEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-22
 * Sends price quotations by e-mail to the customer on demand. The recipient is the quotation
 * contact person, or the customer's own e-mail address when no contact person (or their e-mail)
 * is available. Unlike order notifications, sending a quotation is an explicit user action,
 * therefore a failure to send is reported to the caller.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuotationEmailService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("d.M.yyyy");

    /** Czech labels of the quotation statuses used in the e-mails. */
    private static final Map<QuotationEntity.QuotationStatus, String> STATUS_LABELS = Map.of(
            QuotationEntity.QuotationStatus.NEW, "Nová",
            QuotationEntity.QuotationStatus.SENT, "Odeslaná",
            QuotationEntity.QuotationStatus.ACCEPTED, "Přijatá",
            QuotationEntity.QuotationStatus.REJECTED, "Odmítnutá",
            QuotationEntity.QuotationStatus.IN_PROGRESS, "V realizaci",
            QuotationEntity.QuotationStatus.CANCELLED, "Stornovaná");
    public static final String TD_TD = "</td><td>";

    private final JavaMailSender mailSender;

    /**
     * Sends the quotation to the customer.
     *
     * @throws IllegalStateException when no recipient e-mail address is available or the sending fails
     */
    public void sendQuotation(QuotationEntity quotation) {
        String recipient = resolveRecipient(quotation);
        if (recipient == null) {
            throw new IllegalStateException("Quotation " + quotation.getQuotationNumber()
                    + " has no customer e-mail address, it cannot be sent");
        }
        String subject = "nCRM – cenová nabídka " + quotation.getQuotationNumber();
        try {
            var message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setTo(recipient);
            helper.setSubject(subject);
            helper.setText("<p>Dobrý den,</p><p>zasíláme Vám cenovou nabídku <strong>%s</strong>.</p>"
                    .formatted(quotation.getQuotationNumber()) + buildQuotationSummary(quotation), true);
            mailSender.send(message);
            log.info("Quotation '{}' sent to {}", subject, recipient);
        } catch (Exception e) {
            log.error("Failed to send quotation '{}' to {}", subject, recipient, e);
            throw new IllegalStateException("Failed to send quotation " + quotation.getQuotationNumber()
                    + " to " + recipient, e);
        }
    }

    /** Recipient of the quotation: the contact person's e-mail, or the customer's e-mail as a fallback. */
    private String resolveRecipient(QuotationEntity quotation) {
        if (quotation.getContactPerson() != null && isNotBlank(quotation.getContactPerson().getEmail())) {
            return quotation.getContactPerson().getEmail();
        }
        if (quotation.getCustomer() != null && isNotBlank(quotation.getCustomer().getEmail())) {
            return quotation.getCustomer().getEmail();
        }
        return null;
    }

    /** Builds the HTML summary of the quotation (header data and the item table with totals). */
    private String buildQuotationSummary(QuotationEntity quotation) {
        StringBuilder html = new StringBuilder();
        html.append("<p>Číslo nabídky: <strong>").append(quotation.getQuotationNumber()).append("</strong><br/>");
        if (quotation.getQuotationDate() != null) {
            html.append("Datum nabídky: ").append(quotation.getQuotationDate().format(DATE_FORMAT)).append("<br/>");
        }
        if (quotation.getValidUntil() != null) {
            html.append("Platnost do: ").append(quotation.getValidUntil().format(DATE_FORMAT)).append("<br/>");
        }
        html.append("Stav: ").append(statusLabel(quotation.getStatus())).append("</p>");
        html.append("<table border=\"1\" cellpadding=\"4\" cellspacing=\"0\">")
                .append("<tr><th>Položka</th><th>Množství</th><th>Cena/ks</th><th>Celkem</th></tr>");
        for (QuotationItemEntity item : quotation.getItems()) {
            html.append("<tr><td>").append(item.getItem() == null ? "" : item.getItem().getName())
                    .append(TD_TD).append(item.getQuantity())
                    .append(TD_TD).append(item.getUnitPrice())
                    .append(TD_TD).append(item.getTotalPrice()).append("</td></tr>");
        }
        html.append("</table>");
        html.append("<p>Celková cena: <strong>").append(quotation.getTotalPrice());
        if (quotation.getCurrency() != null) {
            html.append(" ").append(quotation.getCurrency());
        }
        html.append("</strong></p>");
        if (isNotBlank(quotation.getNote())) {
            html.append("<p>Poznámka: ").append(quotation.getNote()).append("</p>");
        }
        return html.toString();
    }

    private String statusLabel(QuotationEntity.QuotationStatus status) {
        return status == null ? "" : STATUS_LABELS.getOrDefault(status, status.name());
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }
}
