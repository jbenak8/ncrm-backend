package cz.jbenak.ncrm_backend.controler;

import cz.jbenak.ncrm_backend.model.dto.security.RoleDto;
import cz.jbenak.ncrm_backend.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-14
 * REST API exposing the security roles assignable to user accounts. Used by the frontend
 * when creating or editing a user; restricted to administrators and the owner.
 */
@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public List<RoleDto> findAll() {
        return userService.findAllRoles();
    }
}
