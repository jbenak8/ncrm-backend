package cz.jbenak.ncrm_backend.services;

import cz.jbenak.ncrm_backend.model.entity.order.OrderEntity;
import cz.jbenak.ncrm_backend.model.entity.order.OrderItemEntity;
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
 * @since 2026-07-16
 * Sends e-mail notifications about orders to the customer: a newly created order, an updated order
 * and a change of the order status. The recipient is the order contact person, or the customer's
 * own e-mail address when no contact person (or their e-mail) is available. A failure to send the
 * notification is only logged and never breaks the order operation itself.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderEmailService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("d.M.yyyy");

    /** Czech labels of the order statuses used in the notification e-mails. */
    private static final Map<OrderEntity.OrderStatus, String> STATUS_LABELS = Map.of(
            OrderEntity.OrderStatus.NEW, "Nová",
            OrderEntity.OrderStatus.CONFIRMED, "Potvrzená",
            OrderEntity.OrderStatus.IN_PROGRESS, "V realizaci",
            OrderEntity.OrderStatus.COMPLETED, "Dokončená",
            OrderEntity.OrderStatus.CANCELLED, "Stornovaná");
    public static final String TD_TD = "</td><td>";

    private final JavaMailSender mailSender;

    /** Notifies the customer about a newly created order. */
    public void sendOrderCreated(OrderEntity order) {
        send(order, "nCRM – nová objednávka " + order.getOrderNumber(),
                "<p>Dobrý den,</p><p>Vaše objednávka <strong>%s</strong> byla přijata.</p>"
                        .formatted(order.getOrderNumber()));
    }

    /** Notifies the customer about a change of an existing order. */
    public void sendOrderUpdated(OrderEntity order) {
        send(order, "nCRM – změna objednávky " + order.getOrderNumber(),
                "<p>Dobrý den,</p><p>Vaše objednávka <strong>%s</strong> byla upravena. Aktuální podoba objednávky:</p>"
                        .formatted(order.getOrderNumber()));
    }

    /** Notifies the customer about a change of the order status. */
    public void sendOrderStatusChanged(OrderEntity order, OrderEntity.OrderStatus previousStatus) {
        send(order, "nCRM – změna stavu objednávky " + order.getOrderNumber(),
                "<p>Dobrý den,</p><p>Stav Vaší objednávky <strong>%s</strong> se změnil z <strong>%s</strong> na <strong>%s</strong>.</p>"
                        .formatted(order.getOrderNumber(), statusLabel(previousStatus), statusLabel(order.getStatus())));
    }

    private void send(OrderEntity order, String subject, String introduction) {
        String recipient = resolveRecipient(order);
        if (recipient == null) {
            log.warn("Order {} has no customer e-mail address, notification will not be sent", order.getOrderNumber());
            return;
        }
        try {
            var message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setTo(recipient);
            helper.setSubject(subject);
            helper.setText(introduction + buildOrderSummary(order), true);
            mailSender.send(message);
            log.info("Order notification '{}' sent to {}", subject, recipient);
        } catch (Exception e) {
            log.error("Failed to send order notification '{}' to {}", subject, recipient, e);
        }
    }

    /** Recipient of the notification: the contact person's e-mail, or the customer's e-mail as a fallback. */
    private String resolveRecipient(OrderEntity order) {
        if (order.getContactPerson() != null && isNotBlank(order.getContactPerson().getEmail())) {
            return order.getContactPerson().getEmail();
        }
        if (order.getCustomer() != null && isNotBlank(order.getCustomer().getEmail())) {
            return order.getCustomer().getEmail();
        }
        return null;
    }

    /** Builds the HTML summary of the order (header data and the item table with totals). */
    private String buildOrderSummary(OrderEntity order) {
        StringBuilder html = new StringBuilder();
        html.append("<p>Číslo objednávky: <strong>").append(order.getOrderNumber()).append("</strong><br/>");
        if (order.getOrderDate() != null) {
            html.append("Datum objednávky: ").append(order.getOrderDate().format(DATE_FORMAT)).append("<br/>");
        }
        html.append("Stav: ").append(statusLabel(order.getStatus())).append("</p>");
        html.append("<table border=\"1\" cellpadding=\"4\" cellspacing=\"0\">")
                .append("<tr><th>Položka</th><th>Množství</th><th>Cena/ks</th><th>Celkem</th></tr>");
        for (OrderItemEntity item : order.getItems()) {
            html.append("<tr><td>").append(item.getItem() == null ? "" : item.getItem().getName())
                    .append(TD_TD).append(item.getQuantity())
                    .append(TD_TD).append(item.getUnitPrice())
                    .append(TD_TD).append(item.getTotalPrice()).append("</td></tr>");
        }
        html.append("</table>");
        html.append("<p>Celková cena: <strong>").append(order.getTotalPrice());
        if (order.getCurrency() != null) {
            html.append(" ").append(order.getCurrency());
        }
        html.append("</strong></p>");
        if (isNotBlank(order.getNote())) {
            html.append("<p>Poznámka: ").append(order.getNote()).append("</p>");
        }
        return html.toString();
    }

    private String statusLabel(OrderEntity.OrderStatus status) {
        return status == null ? "" : STATUS_LABELS.getOrDefault(status, status.name());
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }
}
