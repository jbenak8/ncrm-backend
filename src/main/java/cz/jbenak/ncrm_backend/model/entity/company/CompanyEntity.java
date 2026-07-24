package cz.jbenak.ncrm_backend.model.entity.company;

import cz.jbenak.ncrm_backend.model.entity.AddressEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.NaturalId;

import java.time.LocalDate;
import java.util.UUID;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2024-06-15
 * Represents a company entity in the system. This is the core entity of the CRM system, containing all the necessary information about a company,
 * including its name, registration details, contact information, and banking details.
 * The entity also includes fields for managing the company's status (active, deleted) and tracking deletion metadata.
 * Based on this entity, all other data in the system is linked to a company, making it a central part of the CRM system's data model.
 */

@Getter
@Setter
@ToString
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "companies")
public class CompanyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false, unique = true)
    private UUID id;

    @NonNull
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "name_second_line")
    private String nameSecondLine;

    @NaturalId
    @Column(name = "registration_id", nullable = false, unique = true)
    private String registrationId;

    @NaturalId
    @Column(name = "vat_id", unique = true)
    private String vatId;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "address_id")
    @ToString.Exclude
    private AddressEntity address;

    @Column(name = "registration_note", length = Integer.MAX_VALUE)
    private String registrationNote;

    @Column(name = "registration_note_en", length = Integer.MAX_VALUE)
    private String registrationNoteEn;

    @Column(name = "phone")
    private String phone;

    @Column(name = "email")
    private String email;

    @Column(name = "website")
    private String website;

    @Column(name = "bank_account")
    private String bankAccount;

    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "iban")
    private String iban;

    @Column(name = "bic")
    private String bic;

    @Column(name = "logo")
    @ToString.Exclude
    private byte[] logo;

    @Column(name = "logo_content_type", length = 100)
    private String logoContentType;

    @Column(name = "stamp")
    @ToString.Exclude
    private byte[] stamp;

    @Column(name = "stamp_content_type", length = 100)
    private String stampContentType;

    @Column(name = "active")
    private boolean active = true;

    @Column(name = "deleted")
    private boolean deleted = false;

    @Column(name = "deleted_at")
    private LocalDate deletedAt;

    @Column(name = "deleted_by")
    private String deletedBy;

    @Column(name = "default_company")
    private boolean defaultCompany = false;
}
