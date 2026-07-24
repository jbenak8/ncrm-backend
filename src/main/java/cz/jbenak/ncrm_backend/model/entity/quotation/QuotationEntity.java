package cz.jbenak.ncrm_backend.model.entity.quotation;

import cz.jbenak.ncrm_backend.model.entity.AuditableEntity;
import cz.jbenak.ncrm_backend.model.entity.customer.ContactPersonEntity;
import cz.jbenak.ncrm_backend.model.entity.customer.CustomerEntity;
import cz.jbenak.ncrm_backend.model.entity.company.CompanyEntity;
import cz.jbenak.ncrm_backend.model.entity.company.SalesRepresentativeEntity;
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
 * @since 2026-07-22
 * Represents a price quotation created by a sales representative for a customer. The quotation
 * aggregates quotation items whose unit prices may be manually adjusted, tracks its lifecycle
 * via {@link QuotationStatus} and can be converted into a customer order.
 */

@Getter
@Setter
@ToString
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "quotations")
public class QuotationEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private UUID id;

    @NaturalId
    @Column(name = "quotation_number", nullable = false, unique = true, length = 50)
    private String quotationNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    @ToString.Exclude
    private CustomerEntity customer;

    // Own company that issued the quotation.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    @ToString.Exclude
    private CompanyEntity company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_person_id")
    @ToString.Exclude
    private ContactPersonEntity contactPerson;

    // Sales representative who created / is responsible for the quotation.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_representative_id", nullable = false)
    @ToString.Exclude
    private SalesRepresentativeEntity salesRepresentative;

    @Column(name = "quotation_date", nullable = false)
    private LocalDate quotationDate;

    // Date until which the quoted prices are valid; optional.
    @Column(name = "valid_until")
    private LocalDate validUntil;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private QuotationStatus status;

    @Column(name = "total_price", precision = 14, scale = 2)
    private BigDecimal totalPrice;

    // ISO 4217 currency code of the quotation totals, e.g. "CZK", "EUR".
    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "note")
    private String note;

    // Order created from this quotation, when it has been converted.
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    @ToString.Exclude
    private OrderEntity order;

    @OneToMany(mappedBy = "quotation", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @Builder.Default
    private List<QuotationItemEntity> items = new ArrayList<>();

    /**
     * Convenience method keeping both sides of the quotation-item relation in sync.
     */
    public void addItem(QuotationItemEntity item) {
        item.setQuotation(this);
        items.add(item);
    }

    public enum QuotationStatus {
        NEW, SENT, ACCEPTED, REJECTED, IN_PROGRESS, CANCELLED
    }
}
