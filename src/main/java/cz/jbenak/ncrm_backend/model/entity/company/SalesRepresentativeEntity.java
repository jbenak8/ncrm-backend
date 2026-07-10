package cz.jbenak.ncrm_backend.model.entity.company;

import cz.jbenak.ncrm_backend.model.entity.AuditableEntity;
import cz.jbenak.ncrm_backend.model.entity.security.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.NaturalId;

import java.util.UUID;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-10
 * Represents a sales representative of the company who conducts meetings with customers and creates orders.
 * Every sales representative is also an application user (one-to-one link to {@link UserEntity}),
 * so they can log in to the system. Business contact data (phone, business email) may differ from the login account data.
 */

@Getter
@Setter
@ToString
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "sales_representatives")
public class SalesRepresentativeEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private UUID id;

    // Short internal code of the representative, e.g. used on order documents.
    @NaturalId
    @Column(name = "code", nullable = false, unique = true, length = 20)
    private String code;

    // The user account of the sales representative; every representative is also a user of the system.
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @ToString.Exclude
    private UserEntity user;

    @Column(name = "phone")
    private String phone;

    // Business e-mail presented to customers; may differ from the login e-mail of the user account.
    @Column(name = "business_email")
    private String businessEmail;

    // Sales region or territory the representative is responsible for.
    @Column(name = "region", length = 100)
    private String region;

    @Column(name = "note")
    private String note;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;
}
