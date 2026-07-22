package cz.jbenak.ncrm_backend.model.entity.quotation;

import cz.jbenak.ncrm_backend.model.entity.store.ItemEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-22
 * Represents a single line of a price quotation. The unit price defaults to a snapshot of the item price
 * at the time of quoting, but may be manually adjusted, as may the line total.
 */

@Getter
@Setter
@ToString
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "quotation_items")
public class QuotationItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quotation_id", nullable = false)
    @ToString.Exclude
    private QuotationEntity quotation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    @ToString.Exclude
    private ItemEntity item;

    @Column(name = "quantity", nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity;

    // Unit price of the quotation line; snapshot of the item price, possibly manually adjusted.
    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "total_price", precision = 14, scale = 2)
    private BigDecimal totalPrice;
}
