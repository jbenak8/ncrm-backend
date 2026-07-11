package cz.jbenak.ncrm_backend.controler;

import cz.jbenak.ncrm_backend.model.dto.company.SalesRepresentativeDto;
import cz.jbenak.ncrm_backend.model.dto.security.UserDto;
import cz.jbenak.ncrm_backend.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-11
 * REST API for user administration. Listing and account state changes are restricted to the owner;
 * every authenticated user can read their own profile.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('OWNER')")
    public List<UserDto> findAll() {
        return userService.findAll();
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public UserDto me(Authentication authentication) {
        return userService.findByUsername(authentication.getName());
    }

    @GetMapping("/sales-representatives")
    @PreAuthorize("hasAnyRole('OWNER', 'SALES_REPRESENTATIVE')")
    public List<SalesRepresentativeDto> findAllSalesRepresentatives() {
        return userService.findAllSalesRepresentatives();
    }

    @PostMapping("/{id}/locked/{locked}")
    @PreAuthorize("hasRole('OWNER')")
    public UserDto setLocked(@PathVariable UUID id, @PathVariable boolean locked) {
        return userService.setLocked(id, locked);
    }

    @PostMapping("/{id}/enabled/{enabled}")
    @PreAuthorize("hasRole('OWNER')")
    public UserDto setEnabled(@PathVariable UUID id, @PathVariable boolean enabled) {
        return userService.setEnabled(id, enabled);
    }
}
