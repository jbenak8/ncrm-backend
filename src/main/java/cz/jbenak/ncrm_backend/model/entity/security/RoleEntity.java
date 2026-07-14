package cz.jbenak.ncrm_backend.model.entity.security;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.NaturalId;

import java.util.UUID;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-10
 * Represents a security role used for role-based access control (RBAC).
 * Role names are stored without the "ROLE_" prefix, e.g. "ADMIN", "SALES_REPRESENTATIVE", "CUSTOMER".
 * The "ROLE_" prefix is added by Spring Security only when granted authorities are created from JWT claims.
 */

@Getter
@Setter
@ToString
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "roles")
public class RoleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private UUID id;

    @NaturalId
    @Column(name = "name", nullable = false, unique = true, length = 50)
    private String name;

    @Column(name = "description")
    private String description;
}
