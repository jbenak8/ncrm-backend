package cz.jbenak.ncrm_backend.security;

import cz.jbenak.ncrm_backend.model.entity.security.RoleEntity;
import cz.jbenak.ncrm_backend.model.entity.security.UserEntity;
import cz.jbenak.ncrm_backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Unit tests of {@link DbUserDetailsService} verifying the mapping of database user accounts
 * to Spring Security UserDetails (ROLE_* authorities and account state flags).
 */
@ExtendWith(MockitoExtension.class)
class DbUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DbUserDetailsService dbUserDetailsService;

    private static UserEntity user(boolean enabled, boolean locked, boolean credentialsExpired) {
        return UserEntity.builder()
                .username("owner")
                .email("owner@example.com")
                .passwordHash("{bcrypt}hash")
                .firstName("Otto")
                .lastName("Owner")
                .enabled(enabled)
                .locked(locked)
                .credentialsExpired(credentialsExpired)
                .roles(Set.of(RoleEntity.builder().name("OWNER").build()))
                .build();
    }

    @Test
    void loadsUserWithRoleAuthoritiesAndPasswordHash() {
        when(userRepository.findByUsername("owner")).thenReturn(Optional.of(user(true, false, false)));

        UserDetails details = dbUserDetailsService.loadUserByUsername("owner");

        assertThat(details.getUsername()).isEqualTo("owner");
        assertThat(details.getPassword()).isEqualTo("{bcrypt}hash");
        assertThat(details.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_OWNER");
        assertThat(details.isEnabled()).isTrue();
        assertThat(details.isAccountNonLocked()).isTrue();
        assertThat(details.isCredentialsNonExpired()).isTrue();
    }

    @Test
    void accountStateFlagsAreMapped() {
        when(userRepository.findByUsername("owner")).thenReturn(Optional.of(user(false, true, true)));

        UserDetails details = dbUserDetailsService.loadUserByUsername("owner");

        assertThat(details.isEnabled()).isFalse();
        assertThat(details.isAccountNonLocked()).isFalse();
        assertThat(details.isCredentialsNonExpired()).isFalse();
    }

    @Test
    void unknownUserIsRejected() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dbUserDetailsService.loadUserByUsername("ghost"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
