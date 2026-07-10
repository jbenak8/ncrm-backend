package cz.jbenak.ncrm_backend.model.entity.store;

import cz.jbenak.ncrm_backend.model.entity.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.NaturalId;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-10
 * Represents an item category organized in a tree structure using the adjacency list pattern (self-referencing parent).
 * This allows unlimited nesting of subcategories, e.g.:
 * 1. Paper -> 1.1 Hygienic -> 1.1.1 Toilet paper, 1.1.2 Tissues; 1.2 Office -> 1.2.1 A4
 * 2. Steel -> 2.1 Alloyed -> 2.1.1 Fittings
 * The 'path' field holds the materialized hierarchical code (e.g. "1.1.2") for fast lookups and ordering of the whole subtree.
 */

@Getter
@Setter
@ToString
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "item_categories", uniqueConstraints = {
        @UniqueConstraint(name = "uc_item_categories_parent_name", columnNames = {"parent_id", "name"})
})
public class ItemCategoryEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private UUID id;

    @NaturalId
    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    // Materialized path of hierarchical codes, e.g. "1.1.2" for "Paper / Hygienic / Tissues". Enables efficient subtree queries and natural ordering.
    @Column(name = "path", nullable = false, length = 255)
    private String path;

    // Parent category; null for root (top-level) categories.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    @ToString.Exclude
    private ItemCategoryEntity parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC, name ASC")
    @ToString.Exclude
    @Builder.Default
    private List<ItemCategoryEntity> children = new ArrayList<>();

    // Order of the category among its siblings.
    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private int sortOrder = 0;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    /**
     * Convenience method keeping both sides of the parent-child relation in sync.
     */
    public void addChild(ItemCategoryEntity child) {
        child.setParent(this);
        children.add(child);
    }

    /**
     * @return true if this category is a root (top-level) category.
     */
    public boolean isRoot() {
        return parent == null;
    }
}
