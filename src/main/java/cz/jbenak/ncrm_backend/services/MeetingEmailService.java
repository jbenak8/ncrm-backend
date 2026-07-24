package cz.jbenak.ncrm_backend.services;

import cz.jbenak.ncrm_backend.model.entity.AddressEntity;
import cz.jbenak.ncrm_backend.model.entity.customer.MeetingEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-16
 * Sends calendar invitations (an .ics attachment) for planned meetings to the customer and to the
 * sales representative whenever a meeting is created or updated. The customer recipient is the
 * meeting contact person, or the customer's own e-mail address when no contact person (or their
 * e-mail) is available. A failure to send the invitation is only logged and never breaks the
 * meeting operation itself.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MeetingEmailService {

    private static final DateTimeFormatter ICS_UTC_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("d.M.yyyy H:mm");

    /** Default duration of a meeting used for the calendar event, in minutes. */
    private static final int DEFAULT_DURATION_MINUTES = 60;

    /** Line separator of iCalendar content lines as required by RFC 5545. */
    private static final String CRLF = "\r\n";

    private final JavaMailSender mailSender;

    /** Sends the calendar invitation for a newly created meeting. */
    public void sendMeetingCreated(MeetingEntity meeting) {
        send(meeting, "nCRM – pozvánka na schůzku: " + meeting.getSubject(),
                "<p>Dobrý den,</p><p>byla naplánována schůzka <strong>%s</strong> na <strong>%s</strong>. Pozvánka do kalendáře je v příloze.</p>"
                        .formatted(escapeHtml(meeting.getSubject()), formatDate(meeting.getPlannedDate())), 0);
    }

    /** Sends the updated calendar invitation for a changed meeting. */
    public void sendMeetingUpdated(MeetingEntity meeting) {
        send(meeting, "nCRM – změna schůzky: " + meeting.getSubject(),
                "<p>Dobrý den,</p><p>schůzka <strong>%s</strong> byla upravena, nyní je naplánována na <strong>%s</strong>. Aktualizovaná pozvánka do kalendáře je v příloze.</p>"
                        .formatted(escapeHtml(meeting.getSubject()), formatDate(meeting.getPlannedDate())), 1);
    }

    private void send(MeetingEntity meeting, String subject, String introduction, int sequence) {
        Set<String> recipients = resolveRecipients(meeting);
        if (recipients.isEmpty()) {
            log.warn("Meeting {} has no recipient e-mail address, invitation will not be sent", meeting.getId());
            return;
        }
        try {
            var message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(recipients.toArray(String[]::new));
            helper.setSubject(subject);
            helper.setText(introduction + buildMeetingSummary(meeting), true);
            byte[] ics = buildIcs(meeting, sequence).getBytes(StandardCharsets.UTF_8);
            helper.addAttachment("schuzka.ics", new ByteArrayResource(ics), "text/calendar; charset=UTF-8; method=REQUEST");
            mailSender.send(message);
            log.info("Meeting invitation '{}' sent to {}", subject, recipients);
        } catch (Exception e) {
            log.error("Failed to send meeting invitation '{}' to {}", subject, recipients, e);
        }
    }

    /**
     * Recipients of the invitation: the customer side (the contact person's e-mail, or the customer's
     * e-mail as a fallback) and the sales representative (the business e-mail, or the e-mail of their
     * user account as a fallback).
     */
    private Set<String> resolveRecipients(MeetingEntity meeting) {
        Set<String> recipients = new LinkedHashSet<>();
        if (meeting.getContactPerson() != null && isNotBlank(meeting.getContactPerson().getEmail())) {
            recipients.add(meeting.getContactPerson().getEmail());
        } else if (meeting.getCustomer() != null && isNotBlank(meeting.getCustomer().getEmail())) {
            recipients.add(meeting.getCustomer().getEmail());
        }
        var representative = meeting.getSalesRepresentative();
        if (representative != null) {
            if (isNotBlank(representative.getBusinessEmail())) {
                recipients.add(representative.getBusinessEmail());
            } else if (representative.getUser() != null && isNotBlank(representative.getUser().getEmail())) {
                recipients.add(representative.getUser().getEmail());
            }
        }
        return recipients;
    }

    /** Builds the HTML summary of the meeting (subject, date, place and participants). */
    private String buildMeetingSummary(MeetingEntity meeting) {
        StringBuilder html = new StringBuilder();
        html.append("<p>Předmět: <strong>").append(escapeHtml(meeting.getSubject())).append("</strong><br/>");
        html.append("Termín: ").append(formatDate(meeting.getPlannedDate())).append("<br/>");
        if (meeting.getCustomer() != null) {
            html.append("Zákazník: ").append(escapeHtml(meeting.getCustomer().getName())).append("<br/>");
        }
        String location = resolveLocation(meeting);
        if (isNotBlank(location)) {
            html.append("Místo: ").append(escapeHtml(location)).append("<br/>");
        }
        html.append("</p>");
        if (isNotBlank(meeting.getDescription())) {
            html.append("<p>").append(escapeHtml(meeting.getDescription())).append("</p>");
        }
        return html.toString();
    }

    /** Builds the iCalendar (RFC 5545) invitation for the meeting. */
    private String buildIcs(MeetingEntity meeting, int sequence) {
        ZonedDateTime start = meeting.getPlannedDate().atZone(ZoneId.systemDefault());
        ZonedDateTime end = start.plusMinutes(DEFAULT_DURATION_MINUTES);
        List<String> lines = new ArrayList<>();
        lines.add("BEGIN:VCALENDAR");
        lines.add("VERSION:2.0");
        lines.add("PRODID:-//nCRM//Meetings//CS");
        lines.add("CALSCALE:GREGORIAN");
        lines.add("METHOD:REQUEST");
        lines.add("BEGIN:VEVENT");
        lines.add("UID:" + meeting.getId() + "@ncrm");
        lines.add("SEQUENCE:" + sequence);
        lines.add("DTSTAMP:" + ICS_UTC_FORMAT.format(ZonedDateTime.now(ZoneOffset.UTC)));
        lines.add("DTSTART:" + ICS_UTC_FORMAT.format(start.withZoneSameInstant(ZoneOffset.UTC)));
        lines.add("DTEND:" + ICS_UTC_FORMAT.format(end.withZoneSameInstant(ZoneOffset.UTC)));
        lines.add("SUMMARY:" + escapeIcs(meeting.getSubject()));
        if (isNotBlank(meeting.getDescription())) {
            lines.add("DESCRIPTION:" + escapeIcs(meeting.getDescription()));
        }
        String location = resolveLocation(meeting);
        if (isNotBlank(location)) {
            lines.add("LOCATION:" + escapeIcs(location));
        }
        lines.add("STATUS:" + (meeting.getStatus() == MeetingEntity.MeetingStatus.CANCELLED ? "CANCELLED" : "CONFIRMED"));
        lines.add("END:VEVENT");
        lines.add("END:VCALENDAR");
        StringBuilder ics = new StringBuilder();
        for (String line : lines) {
            ics.append(foldIcsLine(line)).append(CRLF);
        }
        return ics.toString();
    }

    /** Place of the meeting: the customer site (name and address), or the customer's headquarters address. */
    private String resolveLocation(MeetingEntity meeting) {
        if (meeting.getCustomerSite() != null) {
            String address = formatAddress(meeting.getCustomerSite().getAddress());
            return isNotBlank(address)
                    ? meeting.getCustomerSite().getName() + ", " + address
                    : meeting.getCustomerSite().getName();
        }
        if (meeting.getCustomer() != null) {
            return formatAddress(meeting.getCustomer().getHeadquartersAddress());
        }
        return null;
    }

    private String formatAddress(AddressEntity address) {
        if (address == null) {
            return null;
        }
        List<String> parts = new ArrayList<>();
        if (isNotBlank(address.getStreetNumber())) {
            parts.add(address.getStreetNumber());
        } else if (isNotBlank(address.getStreet())) {
            parts.add(isNotBlank(address.getHouseNumber())
                    ? address.getStreet() + " " + address.getHouseNumber()
                    : address.getStreet());
        }
        if (isNotBlank(address.getZipCode()) || isNotBlank(address.getCity())) {
            parts.add(((address.getZipCode() == null ? "" : address.getZipCode() + " ")
                    + (address.getCity() == null ? "" : address.getCity())).trim());
        }
        return parts.isEmpty() ? null : String.join(", ", parts);
    }

    private String formatDate(LocalDateTime date) {
        return date == null ? "" : date.format(DATE_TIME_FORMAT);
    }

    /** Escapes special characters of iCalendar text values as required by RFC 5545. */
    private String escapeIcs(String value) {
        return value == null ? "" : value
                .replace("\\", "\\\\")
                .replace(";", "\\;")
                .replace(",", "\\,")
                .replace(CRLF, "\\n")
                .replace("\n", "\\n");
    }

    /** Folds an iCalendar content line to the maximum length of 75 octets (RFC 5545, section 3.1). */
    private String foldIcsLine(String line) {
        if (line.length() <= 75) {
            return line;
        }
        StringBuilder folded = new StringBuilder();
        int index = 0;
        while (index < line.length()) {
            int end = Math.min(index + 75, line.length());
            if (index > 0) {
                folded.append(CRLF).append(' ');
            }
            folded.append(line, index, end);
            index = end;
        }
        return folded.toString();
    }

    private String escapeHtml(String value) {
        return value == null ? "" : value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }
}
