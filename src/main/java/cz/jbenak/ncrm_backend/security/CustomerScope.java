package cz.jbenak.ncrm_backend.security;

import cz.jbenak.ncrm_backend.model.entity.customer.CustomerEntity;
import cz.jbenak.ncrm_backend.model.entity.security.UserEntity;
import cz.jbenak.ncrm_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-24
 * Resolves the data scope of customer user accounts. A pure customer account (the
 * {@code CUSTOMER} role without any back-office role) may only read the data belonging
 * to the customer record linked to the account.
 */
@Component
@RequiredArgsConstructor
public class CustomerScope {

    /** Authorities of back-office users that are not restricted to a single customer. */
    private static final Set<String> BACK_OFFICE_AUTHORITIES =
            Set.of("ROLE_ADMIN", "ROLE_OWNER", "ROLE_SALES_REPRESENTATIVE");

    private final UserRepository userRepository;

    /**
     * Returns {@code true} when the authenticated user is a pure customer account,
     * i.e. has the {@code CUSTOMER} role and no back-office role.
     */
    public boolean isCustomer(Authentication authentication) {
        boolean customer = false;
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (BACK_OFFICE_AUTHORITIES.contains(authority.getAuthority())) {
                return false;
            }
            customer |= "ROLE_CUSTOMER".equals(authority.getAuthority());
        }
        return customer;
    }

    /**
     * Returns the id of the customer record linked to the authenticated user account,
     * or an empty optional when the account is not linked to any customer.
     */
    @Transactional(readOnly = true)
    public Optional<UUID> customerId(Authentication authentication) {
        return userRepository.findByUsername(authentication.getName())
                .map(UserEntity::getCustomer)
                .map(CustomerEntity::getId);
    }

    /**
     * Returns the given search filters restricted to the customer of the authenticated user.
     * For back-office users the filters are returned unchanged; for a pure customer account a
     * {@code customer.id:eq:<id>} filter is enforced (an account without a linked customer
     * matches nothing).
     */
    @Transactional(readOnly = true)
    public List<String> scopedFilters(List<String> filters, Authentication authentication) {
        if (!isCustomer(authentication)) {
            return filters;
        }
        UUID customerId = customerId(authentication).orElse(new UUID(0L, 0L));
        List<String> scoped = new ArrayList<>(filters == null ? List.of() : filters);
        scoped.add("customer.id:eq:" + customerId);
        return scoped;
    }
}
