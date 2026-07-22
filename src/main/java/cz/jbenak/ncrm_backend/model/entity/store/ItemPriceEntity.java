package cz.jbenak.ncrm_backend.model.entity.store;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-10
 * Represents the price of an item, including currency, applicable VAT rate and validity period.
 */

@Getter
@Setter
@ToString
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "item_prices")
public class ItemPriceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false, unique = true)
    @ToString.Exclude
    private ItemEntity item;

    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    // ISO 4217 currency code, e.g. "CZK", "EUR".
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    // Purchase price of the item excluding VAT.
    @Column(name = "purchase_price_net", precision = 12, scale = 2)
    private BigDecimal purchasePriceNet;

    @Column(name = "vat_rate", precision = 5, scale = 2)
    private BigDecimal vatRate;

    @Column(name = "valid_from")
    private LocalDateTime validFrom;

    @Column(name = "valid_to")
    private LocalDateTime validTo;
}
