package cz.jbenak.ncrm_backend.model.entity.customer;

import cz.jbenak.ncrm_backend.model.entity.AddressEntity;
import cz.jbenak.ncrm_backend.model.entity.AuditableEntity;
import cz.jbenak.ncrm_backend.model.entity.company.SalesRepresentativeEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.NaturalId;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@ToString
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "customers")
public class CustomerEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private UUID id;

    @NaturalId(mutable = true)
    @Column(name = "designation")
    private String designation;

    @Column(name = "name")
    private String name;

    @NaturalId(mutable = true)
    @Column(name = "registration_id", unique = true, nullable = false)
    private String registrationId;

    @NaturalId(mutable = true)
    @Column(name = "vat_id", unique = true)
    private String vatId;

    @Column(name = "email")
    private String email;

    @Column(name = "phone")
    private String phone;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "headquarters_address_id")
    @ToString.Exclude
    private AddressEntity headquartersAddress;

    // Sales representative responsible for this customer (account owner).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_representative_id")
    @ToString.Exclude
    private SalesRepresentativeEntity salesRepresentative;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "note", length = Integer.MAX_VALUE)
    private String note;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @Builder.Default
    private List<ContactPersonEntity> contactPersons = new ArrayList<>();

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @Builder.Default
    private List<CustomerSiteEntity> sites = new ArrayList<>();
}
