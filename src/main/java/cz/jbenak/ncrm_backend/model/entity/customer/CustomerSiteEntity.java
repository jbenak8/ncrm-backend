package cz.jbenak.ncrm_backend.model.entity.customer;

import cz.jbenak.ncrm_backend.model.entity.AddressEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-10
 * Represents a physical site (branch, plant, warehouse) of a customer where meetings can take place
 * or goods can be delivered.
 */

@Getter
@Setter
@ToString
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "customer_sites")
public class CustomerSiteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    @ToString.Exclude
    private CustomerEntity customer;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "address_id")
    @ToString.Exclude
    private AddressEntity address;

    @Column(name = "note")
    private String note;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;
}
