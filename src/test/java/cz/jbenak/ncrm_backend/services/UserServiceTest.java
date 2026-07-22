package cz.jbenak.ncrm_backend.services;

import cz.jbenak.ncrm_backend.model.dto.security.ChangePasswordRequest;
import cz.jbenak.ncrm_backend.model.dto.security.UserDto;
import cz.jbenak.ncrm_backend.model.dto.security.UserRequest;
import cz.jbenak.ncrm_backend.model.entity.company.SalesRepresentativeEntity;
import cz.jbenak.ncrm_backend.model.entity.security.RoleEntity;
import cz.jbenak.ncrm_backend.model.entity.security.UserEntity;
import cz.jbenak.ncrm_backend.model.mapper.UserMapper;
import cz.jbenak.ncrm_backend.repository.CompanyRepository;
import cz.jbenak.ncrm_backend.repository.RoleRepository;
import cz.jbenak.ncrm_backend.repository.SalesRepresentativeRepository;
import cz.jbenak.ncrm_backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests of {@link UserService} covering user listing, profile lookup,
 * login auditing and account state flags (locked / enabled).
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private SalesRepresentativeRepository salesRepresentativeRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private CompanyRepository companyRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private UserService userService;

    @Test
    void findAllDelegatesToRepository() {
        when(userRepository.findAll()).thenReturn(List.of());

        userService.findAll();

        verify(userMapper).toDtoList(List.of());
    }

    @Test
    void findByUsernameThrowsWhenMissing() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findByUsername("ghost"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("ghost");
    }

    @Test
    void findByUsernameMapsExistingUser() {
        UserEntity user = new UserEntity();
        when(userRepository.findByUsername("owner")).thenReturn(Optional.of(user));
        when(userMapper.toDto(user)).thenReturn(mock(UserDto.class));

        userService.findByUsername("owner");

        verify(userMapper).toDto(user);
    }

    @Test
    void findAllSalesRepresentativesUsesActiveOnly() {
        when(salesRepresentativeRepository.findAllByActiveTrue()).thenReturn(List.of());

        userService.findAllSalesRepresentatives();

        verify(salesRepresentativeRepository).findAllByActiveTrue();
    }

    @Test
    void recordLoginStoresTimestamp() {
        UserEntity user = new UserEntity();
        when(userRepository.findByUsername("owner")).thenReturn(Optional.of(user));

        userService.recordLogin("owner");

        assertThat(user.getLastLoginAt()).isNotNull();
        verify(userRepository).save(user);
    }

    @Test
    void recordLoginIgnoresUnknownUser() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        userService.recordLogin("ghost");

        verify(userRepository, never()).save(new UserEntity());
    }

    @Test
    void setLockedUpdatesFlag() {
        UUID id = UUID.randomUUID();
        UserEntity user = new UserEntity();
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        userService.setLocked(id, true);

        assertThat(user.isLocked()).isTrue();
    }

    @Test
    void setEnabledUpdatesFlag() {
        UUID id = UUID.randomUUID();
        UserEntity user = new UserEntity();
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        userService.setEnabled(id, false);

        assertThat(user.isEnabled()).isFalse();
    }

    private UserRequest userRequest(String password) {
        return new UserRequest("john", "john@example.com", password, "John", "Doe", true, false, false, false,
                Set.of("OWNER"), null);
    }

    @Test
    void createEncodesPasswordAndResolvesRoles() {
        when(userRepository.existsByUsername("john")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(userMapper.toEntity(any(UserRequest.class))).thenReturn(new UserEntity());
        when(passwordEncoder.encode("Secret.Password1")).thenReturn("hash");
        RoleEntity role = new RoleEntity();
        role.setName("OWNER");
        when(roleRepository.findByName("OWNER")).thenReturn(Optional.of(role));
        when(userRepository.save(any(UserEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.create(userRequest("Secret.Password1"));

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("hash");
        assertThat(captor.getValue().getRoles()).containsExactly(role);
    }

    @Test
    void createRejectsDuplicateUsername() {
        when(userRepository.existsByUsername("john")).thenReturn(true);

        UserRequest request = userRequest("Secret.Password1");
        assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("john");
    }

    @Test
    void createRejectsMissingPassword() {
        when(userRepository.existsByUsername("john")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);

        UserRequest request = userRequest(null);
        assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Password");
    }

    @Test
    void updateKeepsPasswordWhenBlank() {
        UUID id = UUID.randomUUID();
        UserEntity entity = new UserEntity();
        entity.setId(id);
        entity.setPasswordHash("original-hash");
        when(userRepository.findById(id)).thenReturn(Optional.of(entity));
        when(userRepository.findByUsername("john")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.empty());
        RoleEntity role = new RoleEntity();
        role.setName("OWNER");
        when(roleRepository.findByName("OWNER")).thenReturn(Optional.of(role));
        when(userRepository.save(entity)).thenReturn(entity);

        userService.update(id, userRequest(null));

        assertThat(entity.getPasswordHash()).isEqualTo("original-hash");
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void updateRejectsUsernameOfAnotherUser() {
        UUID id = UUID.randomUUID();
        UserEntity entity = new UserEntity();
        entity.setId(id);
        UserEntity other = new UserEntity();
        other.setId(UUID.randomUUID());
        when(userRepository.findById(id)).thenReturn(Optional.of(entity));
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(other));

        UserRequest request = userRequest(null);
        assertThatThrownBy(() -> userService.update(id, request))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void deleteRemovesUser() {
        UUID id = UUID.randomUUID();
        UserEntity entity = new UserEntity();
        entity.setUsername("john");
        when(userRepository.findById(id)).thenReturn(Optional.of(entity));
        when(salesRepresentativeRepository.findByUserUsername("john")).thenReturn(Optional.empty());

        userService.delete(id);

        verify(userRepository).delete(entity);
    }

    @Test
    void deleteRejectsUserLinkedToRepresentative() {
        UUID id = UUID.randomUUID();
        UserEntity entity = new UserEntity();
        entity.setUsername("john");
        SalesRepresentativeEntity rep = new SalesRepresentativeEntity();
        rep.setCode("REP1");
        when(userRepository.findById(id)).thenReturn(Optional.of(entity));
        when(salesRepresentativeRepository.findByUserUsername("john")).thenReturn(Optional.of(rep));

        assertThatThrownBy(() -> userService.delete(id))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("REP1");
        verify(userRepository, never()).delete(entity);
    }

    @Test
    void setLockedThrowsWhenUserMissing() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.setLocked(id, true))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void createRejectsPasswordViolatingPolicy() {
        when(userRepository.existsByUsername("john")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);

        UserRequest request = userRequest("weakpassword");
        assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Password");
    }

    @Test
    void changePasswordEncodesNewPasswordAndClearsFlags() {
        UserEntity user = new UserEntity();
        user.setUsername("john");
        user.setPasswordHash("old-hash");
        user.setMustChangePassword(true);
        user.setCredentialsExpired(true);
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Old.Password1", "old-hash")).thenReturn(true);
        when(passwordEncoder.encode("New.Password1")).thenReturn("new-hash");
        when(userRepository.save(user)).thenReturn(user);

        userService.changePassword("john", new ChangePasswordRequest("Old.Password1", "New.Password1"));

        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
        assertThat(user.isMustChangePassword()).isFalse();
        assertThat(user.isCredentialsExpired()).isFalse();
    }

    @Test
    void changePasswordRejectsWrongCurrentPassword() {
        UserEntity user = new UserEntity();
        user.setPasswordHash("old-hash");
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Wrong.Password1", "old-hash")).thenReturn(false);

        ChangePasswordRequest request = new ChangePasswordRequest("Wrong.Password1", "New.Password1");
        assertThatThrownBy(() -> userService.changePassword("john", request))
                .isInstanceOf(IllegalArgumentException.class);
        verify(userRepository, never()).save(user);
    }

    @Test
    void changePasswordRejectsNewPasswordViolatingPolicy() {
        UserEntity user = new UserEntity();
        user.setPasswordHash("old-hash");
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Old.Password1", "old-hash")).thenReturn(true);

        ChangePasswordRequest request = new ChangePasswordRequest("Old.Password1", "weakpassword");
        assertThatThrownBy(() -> userService.changePassword("john", request))
                .isInstanceOf(IllegalArgumentException.class);
        verify(userRepository, never()).save(user);
    }
}
