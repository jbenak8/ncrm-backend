package cz.jbenak.ncrm_backend.model.entity.order;

import cz.jbenak.ncrm_backend.model.entity.AuditableEntity;
import cz.jbenak.ncrm_backend.model.entity.customer.ContactPersonEntity;
import cz.jbenak.ncrm_backend.model.entity.customer.CustomerEntity;
import cz.jbenak.ncrm_backend.model.entity.company.CompanyEntity;
import cz.jbenak.ncrm_backend.model.entity.company.SalesRepresentativeEntity;
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
 * @since 2026-07-10
 * Represents a customer order created by a sales representative. The order aggregates order items
 * and tracks its lifecycle via {@link OrderStatus}.
 */

@Getter
@Setter
@ToString
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "orders")
public class OrderEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private UUID id;

    @NaturalId
    @Column(name = "order_number", nullable = false, unique = true, length = 50)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    @ToString.Exclude
    private CustomerEntity customer;

    // Own company that issued the order. Nullable for legacy orders created before the column existed.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    @ToString.Exclude
    private CompanyEntity company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_person_id")
    @ToString.Exclude
    private ContactPersonEntity contactPerson;

    // Sales representative who created / is responsible for the order. Null for orders
    // created directly by a customer user; such orders are handled by the company itself.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_representative_id")
    @ToString.Exclude
    private SalesRepresentativeEntity salesRepresentative;

    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OrderStatus status;

    @Column(name = "total_price", precision = 14, scale = 2)
    private BigDecimal totalPrice;

    // ISO 4217 currency code of the order totals, e.g. "CZK", "EUR".
    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "note")
    private String note;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @Builder.Default
    private List<OrderItemEntity> items = new ArrayList<>();

    /**
     * Convenience method keeping both sides of the order-item relation in sync.
     */
    public void addItem(OrderItemEntity item) {
        item.setOrder(this);
        items.add(item);
    }

    public enum OrderStatus {
        NEW, CONFIRMED, IN_PROGRESS, COMPLETED, CANCELLED
    }
}
