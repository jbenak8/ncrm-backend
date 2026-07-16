package cz.jbenak.ncrm_backend.model.entity.invoice;

import cz.jbenak.ncrm_backend.model.entity.AuditableEntity;
import cz.jbenak.ncrm_backend.model.entity.order.OrderEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.NaturalId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-16
 * Represents an invoice issued for a completed customer order. The invoice is an immutable
 * snapshot of the order at the time of issuing: item names, prices and VAT rates are copied
 * into {@link InvoiceItemEntity} lines so that later changes of the catalogue never affect
 * already issued invoices. The payment can be made in cash or by bank transfer.
 */

@Getter
@Setter
@ToString
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "invoices")
public class InvoiceEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private UUID id;

    @NaturalId
    @Column(name = "invoice_number", nullable = false, unique = true, length = 50)
    private String invoiceNumber;

    // The completed order the invoice was issued for; one order can have at most one invoice.
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    @ToString.Exclude
    private OrderEntity order;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false, length = 20)
    private PaymentType paymentType;

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    // Date of the taxable supply (datum uskutečnění zdanitelného plnění).
    @Column(name = "tax_date", nullable = false)
    private LocalDate taxDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    // Variable symbol of the payment, derived from the digits of the invoice number.
    @Column(name = "variable_symbol", length = 10)
    private String variableSymbol;

    @Column(name = "total_net", precision = 14, scale = 2)
    private BigDecimal totalNet;

    @Column(name = "total_vat", precision = 14, scale = 2)
    private BigDecimal totalVat;

    @Column(name = "total_gross", precision = 14, scale = 2)
    private BigDecimal totalGross;

    // ISO 4217 currency code of the invoice totals, e.g. "CZK", "EUR".
    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "note")
    private String note;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @Builder.Default
    private List<InvoiceItemEntity> items = new ArrayList<>();

    /**
     * Convenience method keeping both sides of the invoice-item relation in sync.
     */
    public void addItem(InvoiceItemEntity item) {
        item.setInvoice(this);
        items.add(item);
    }

    /**
     * Enum representing the payment type of the invoice:
     * - CASH: paid in cash on delivery, no payment QR code is printed.
     * - TRANSFER: paid by bank transfer, the invoice carries the bank account,
     *   variable symbol and a payment QR code (SPD).
     */
    public enum PaymentType {
        CASH, TRANSFER
    }
}
