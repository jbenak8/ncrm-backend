package cz.jbenak.ncrm_backend.model.entity.marketing;

import cz.jbenak.ncrm_backend.model.entity.customer.CustomerEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-11
 * Represents a single recipient (customer) of a marketing campaign. The e-mail address is copied
 * from the customer at the time of adding, and the delivery status is tracked per recipient.
 */

@Getter
@Setter
@ToString
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "campaign_recipients", uniqueConstraints = {
        @UniqueConstraint(name = "uc_campaign_recipients", columnNames = {"campaign_id", "customer_id"})
})
public class CampaignRecipientEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    @ToString.Exclude
    private CampaignEntity campaign;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    @ToString.Exclude
    private CustomerEntity customer;

    // E-mail address snapshot taken from the customer when the recipient was added.
    @Column(name = "email", nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status", nullable = false, length = 20)
    @Builder.Default
    private DeliveryStatus deliveryStatus = DeliveryStatus.PENDING;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "error_message")
    private String errorMessage;

    public enum DeliveryStatus {
        PENDING, SENT, FAILED
    }
}
