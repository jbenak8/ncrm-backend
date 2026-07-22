package cz.jbenak.ncrm_backend.model.entity.customer;

import cz.jbenak.ncrm_backend.model.entity.AuditableEntity;
import cz.jbenak.ncrm_backend.model.entity.company.CompanyEntity;
import cz.jbenak.ncrm_backend.model.entity.company.SalesRepresentativeEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-10
 * Represents a meeting between a sales representative and a customer (optionally at a specific customer site
 * and with a specific contact person). The lifecycle of the meeting is tracked via {@link MeetingStatus}.
 */

@Getter
@Setter
@ToString
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "meetings")
public class MeetingEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    @ToString.Exclude
    private CustomerEntity customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_person_id")
    @ToString.Exclude
    private ContactPersonEntity contactPerson;

    // Own company for which the meeting is held.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    @ToString.Exclude
    private CompanyEntity company;

    // Sales representative who conducts the meeting.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_representative_id", nullable = false)
    @ToString.Exclude
    private SalesRepresentativeEntity salesRepresentative;

    // Optional customer site where the meeting takes place.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_site_id")
    @ToString.Exclude
    private CustomerSiteEntity customerSite;

    @Column(name = "subject", nullable = false)
    private String subject;

    @Column(name = "description")
    private String description;

    @Column(name = "planned_date", nullable = false)
    private LocalDateTime plannedDate;

    @Column(name = "actual_date")
    private LocalDateTime actualDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MeetingStatus status;

    @Column(name = "outcome")
    private String outcome;

    public enum MeetingStatus {
        PLANNED, IN_PROGRESS, COMPLETED, CANCELLED
    }
}
