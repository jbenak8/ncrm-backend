package cz.jbenak.ncrm_backend.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.NaturalId;

@Getter
@Setter
@ToString
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "countries")
public class CountryEntity {

    @Id
    @NaturalId
    @Column(name = "iso_code", nullable = false, unique = true, length = 3)
    private String isoCode;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "name_en", nullable = false)
    private String nameEn;

    @Column(name = "name_local")
    private String nameLocal; // Optional field for the local name of the country.

    @Column(name = "active", nullable = false)
    private boolean active = true;

    // Indicates whether the country is a member of the European Union - used for VAT and other EU-specific regulations.
    @Column(name = "eu_member", nullable = false)
    private boolean euMember = false;

    @Column(name = "embargoed", nullable = false)
    private boolean embargoed = false; // Indicates whether the country is under embargo, which may affect trade and business operations.

    @Column(name = "vat_short_code", length = 10)
    private String vatShortCode; // Optional field for VAT short code, if applicable.

    @Column(name = "dialing_code", nullable = false, length = 4)
    private String dialingCode;
}
