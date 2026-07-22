package cz.jbenak.ncrm_backend.model.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-16
 * Definition of a number sequence used to generate document numbers (orders, invoices).
 * The generated number consists of an optional prefix, an optional year part and a zero-padded
 * counter, e.g. {@code OBJ-2026-000123}. The counter can be reset automatically at the start of a new year.
 */

@Getter
@Setter
@ToString
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "number_sequences")
public class NumberSequenceEntity extends AuditableEntity {

    /**
     * Enum representing the type of documents numbered by the sequence:
     * - ORDER: customer order numbers.
     * - INVOICE: invoice numbers.
     * - QUOTATION: price quotation numbers.
     */
    public enum SequenceType {

        ORDER,
        INVOICE,
        QUOTATION
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false, unique = true)
    private UUID id;

    @Column(name = "type", nullable = false, unique = true, length = 20)
    @Enumerated(EnumType.STRING)
    private SequenceType type;

    @Column(name = "prefix", length = 20)
    private String prefix;

    @Builder.Default
    @Column(name = "include_year", nullable = false)
    private boolean includeYear = true;

    @Builder.Default
    @Column(name = "padding", nullable = false)
    private int padding = 6;

    @Builder.Default
    @Column(name = "next_value", nullable = false)
    private long nextValue = 1L;

    @Builder.Default
    @Column(name = "yearly_reset", nullable = false)
    private boolean yearlyReset = true;

    @Column(name = "last_reset_year")
    private Integer lastResetYear;

    @Column(name = "description")
    private String description;
}
