package cz.jbenak.ncrm_backend.model.entity.store;

import cz.jbenak.ncrm_backend.model.entity.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.NaturalId;

import java.util.UUID;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-10
 * Represents an item (goods or service) offered by the company. Each item belongs to a category
 * (which can be a leaf of the category tree) and has a current price record.
 */

@Getter
@Setter
@ToString
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "items")
public class ItemEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private UUID id;

    @NaturalId
    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", length = Integer.MAX_VALUE)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 20)
    private ItemType itemType;

    // Category of the item within the hierarchical category tree.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    @ToString.Exclude
    private ItemCategoryEntity category;

    @Column(name = "unit", length = 20)
    private String unit;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    // Image of the item shown by the frontend in the catalogue.
    @Column(name = "image")
    @ToString.Exclude
    private byte[] image;

    @Column(name = "image_content_type", length = 100)
    private String imageContentType;

    @OneToOne(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private ItemPriceEntity price;

    /**
     * Enum representing the type of the item.
     */
    public enum ItemType {
        GOODS, SERVICE
    }
}
