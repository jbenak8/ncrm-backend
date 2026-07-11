package cz.jbenak.ncrm_backend.services;

import cz.jbenak.ncrm_backend.model.dto.security.UserDto;
import cz.jbenak.ncrm_backend.model.entity.security.UserEntity;
import cz.jbenak.ncrm_backend.model.mapper.UserMapper;
import cz.jbenak.ncrm_backend.repository.SalesRepresentativeRepository;
import cz.jbenak.ncrm_backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    private UserMapper userMapper;

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

    @Test
    void setLockedThrowsWhenUserMissing() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.setLocked(id, true))
                .isInstanceOf(NotFoundException.class);
    }
}
