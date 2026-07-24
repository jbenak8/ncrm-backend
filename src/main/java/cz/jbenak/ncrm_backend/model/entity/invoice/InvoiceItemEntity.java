package cz.jbenak.ncrm_backend.model.entity.invoice;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-16
 * Represents a single line of an issued invoice. All values (item code, name, unit, price and
 * VAT rate) are snapshots taken from the order and the item catalogue at the time of issuing,
 * so that later changes never affect historical invoices.
 */

@Getter
@Setter
@ToString
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "invoice_items")
public class InvoiceItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    @ToString.Exclude
    private InvoiceEntity invoice;

    @Column(name = "item_code", length = 50)
    private String itemCode;

    @Column(name = "item_name", nullable = false)
    private String itemName;

    @Column(name = "quantity", nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity;

    @Column(name = "unit", length = 20)
    private String unit;

    // Net unit price valid at the time of ordering (snapshot of the order line price).
    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    // VAT rate in percent applied to the line, e.g. 21.00.
    @Column(name = "vat_rate", precision = 5, scale = 2)
    private BigDecimal vatRate;

    @Column(name = "total_net", precision = 14, scale = 2)
    private BigDecimal totalNet;

    @Column(name = "total_vat", precision = 14, scale = 2)
    private BigDecimal totalVat;

    @Column(name = "total_gross", precision = 14, scale = 2)
    private BigDecimal totalGross;
}
