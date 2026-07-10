package cz.jbenak.ncrm_backend.model.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2024-06-01
 * Represents a VAT (Value Added Tax) rate applicable in a specific country. Each VAT rate has a type, a percentage rate, and a validity period defined by the validFrom and validTo fields.
 */

@Getter
@Setter
@ToString
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "vat_rates")
public class VATRate {

    /**
     * Enum representing different types of VAT rates. Usually in EU countries there can be up to 5 different types of VAT rates, but usually only 2 or 3 are used. The most common types are:
     * - BASE: The standard VAT rate is applied to most goods and services.
     * - REDUCED_1: A reduced VAT rate applied to certain goods and services, such as food, books, and medical supplies.
     * - REDUCED_2: A second reduced VAT rate applied to specific goods and services, which may vary by country.
     * - REDUCED_3: A third reduced VAT rate applied to specific goods and services, which may vary by country.
     * - ZERO: A zero VAT rate applied to certain goods and services, such as exports and some medical supplies.
     */
    public enum VATType {

        BASE,
        REDUCED_1,
        REDUCED_2,
        REDUCED_3,
        ZERO
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false, unique = true)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "country_id")
    @ToString.Exclude
    private CountryEntity country;

    @Column(name = "type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private VATType type;

    @Column(name = "rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal rate;

    @Column(name = "valid_from", nullable = false)
    private LocalDateTime validFrom;

    @Column(name = "valid_to")
    private LocalDateTime validTo;
}
