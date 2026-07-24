package cz.jbenak.ncrm_backend.controler;

import cz.jbenak.ncrm_backend.model.dto.company.SalesRepresentativeDto;
import cz.jbenak.ncrm_backend.model.dto.security.ChangePasswordRequest;
import cz.jbenak.ncrm_backend.model.dto.security.UserDto;
import cz.jbenak.ncrm_backend.model.dto.security.UserRequest;
import cz.jbenak.ncrm_backend.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
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
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public List<UserDto> findAll() {
        return userService.findAll();
    }

    /**
     * Generic search endpoint. Accepts repeatable {@code filter} query parameters in the form
     * {@code field:operator:value} (operators: contains, notContains, eq, neq, lt, gt, between;
     * for between the value is {@code lower,upper}). All filters are combined with AND.
     * Example: {@code /api/users/search?filter=username:contains:adm&filter=enabled:eq:true}
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public Page<UserDto> search(@RequestParam(required = false) List<String> filter, Pageable pageable) {
        return userService.search(filter, pageable);
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public UserDto me(Authentication authentication) {
        return userService.findByUsername(authentication.getName());
    }

    /**
     * Changes the password of the currently authenticated user. The current password must match
     * and the new one must satisfy the password policy. Clears the "must change password" flag.
     */
    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public UserDto changePassword(Authentication authentication,
                                  @Valid @RequestBody ChangePasswordRequest request) {
        return userService.changePassword(authentication.getName(), request);
    }

    @GetMapping("/sales-representatives")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'SALES_REPRESENTATIVE')")
    public List<SalesRepresentativeDto> findAllSalesRepresentatives() {
        return userService.findAllSalesRepresentatives();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public UserDto create(@Valid @RequestBody UserRequest request) {
        return userService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public UserDto update(@PathVariable UUID id, @Valid @RequestBody UserRequest request) {
        return userService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public void delete(@PathVariable UUID id) {
        userService.delete(id);
    }

    @PostMapping("/{id}/locked/{locked}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public UserDto setLocked(@PathVariable UUID id, @PathVariable boolean locked) {
        return userService.setLocked(id, locked);
    }

    @PostMapping("/{id}/enabled/{enabled}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public UserDto setEnabled(@PathVariable UUID id, @PathVariable boolean enabled) {
        return userService.setEnabled(id, enabled);
    }
}
