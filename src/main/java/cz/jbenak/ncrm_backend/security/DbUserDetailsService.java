package cz.jbenak.ncrm_backend.security;

import cz.jbenak.ncrm_backend.model.entity.security.UserEntity;
import cz.jbenak.ncrm_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-23
 * Loads application users from the database for the "db-auth" profile, where authentication
 * is performed against the local user accounts (BCrypt password hashes in the "users" table)
 * instead of delegating to Keycloak. Role names stored without the "ROLE_" prefix are mapped
 * to Spring Security ROLE_* authorities; account state flags follow the UserDetails contract.
 */
@Service
@Profile("db-auth")
@RequiredArgsConstructor
public class DbUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User " + username + " not found."));
        List<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role.getName().toUpperCase()))
                .toList();
        return User.withUsername(user.getUsername())
                .password(user.getPasswordHash())
                .authorities(authorities)
                .disabled(!user.isEnabled())
                .accountLocked(user.isLocked())
                .credentialsExpired(user.isCredentialsExpired())
                .build();
    }
}
