package cz.jbenak.ncrm_backend.services;

import cz.jbenak.ncrm_backend.model.dto.company.SalesRepresentativeRequest;
import cz.jbenak.ncrm_backend.model.entity.company.SalesRepresentativeEntity;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests of {@link SalesRepresentativeService} covering listing, creation with duplicate
 * code protection, user account resolution, updates and activation / deactivation.
 */
@ExtendWith(MockitoExtension.class)
class SalesRepresentativeServiceTest {

    @Mock
    private SalesRepresentativeRepository salesRepresentativeRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private SalesRepresentativeService salesRepresentativeService;

    private final UUID userId = UUID.randomUUID();

    private SalesRepresentativeRequest request() {
        return new SalesRepresentativeRequest("REP1", userId, "+420777888999",
                "rep@company.cz", "Praha", null, true);
    }

    @Test
    void findAllDelegatesToRepository() {
        when(salesRepresentativeRepository.findAll()).thenReturn(List.of());

        salesRepresentativeService.findAll();

        verify(userMapper).toRepresentativeDtoList(List.of());
    }

    @Test
    void findByIdThrowsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(salesRepresentativeRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> salesRepresentativeService.findById(id))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void createRejectsDuplicateCode() {
        when(salesRepresentativeRepository.findByCode("REP1")).thenReturn(Optional.of(new SalesRepresentativeEntity()));

        assertThatThrownBy(() -> salesRepresentativeService.create(request()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("REP1");
    }

    @Test
    void createResolvesUserAccount() {
        SalesRepresentativeEntity entity = new SalesRepresentativeEntity();
        UserEntity user = new UserEntity();
        when(salesRepresentativeRepository.findByCode("REP1")).thenReturn(Optional.empty());
        when(userMapper.toEntity(any(SalesRepresentativeRequest.class))).thenReturn(entity);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(salesRepresentativeRepository.save(entity)).thenReturn(entity);

        salesRepresentativeService.create(request());

        assertThat(entity.getUser()).isSameAs(user);
        verify(salesRepresentativeRepository).save(entity);
    }

    @Test
    void createThrowsWhenUserMissing() {
        when(salesRepresentativeRepository.findByCode("REP1")).thenReturn(Optional.empty());
        when(userMapper.toEntity(any(SalesRepresentativeRequest.class))).thenReturn(new SalesRepresentativeEntity());
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> salesRepresentativeService.create(request()))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("User");
    }

    @Test
    void updateRejectsCodeOfAnotherRepresentative() {
        UUID id = UUID.randomUUID();
        SalesRepresentativeEntity entity = new SalesRepresentativeEntity();
        entity.setId(id);
        SalesRepresentativeEntity other = new SalesRepresentativeEntity();
        other.setId(UUID.randomUUID());
        when(salesRepresentativeRepository.findById(id)).thenReturn(Optional.of(entity));
        when(salesRepresentativeRepository.findByCode("REP1")).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> salesRepresentativeService.update(id, request()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void updateAppliesChangesAndUser() {
        UUID id = UUID.randomUUID();
        SalesRepresentativeEntity entity = new SalesRepresentativeEntity();
        entity.setId(id);
        UserEntity user = new UserEntity();
        when(salesRepresentativeRepository.findById(id)).thenReturn(Optional.of(entity));
        when(salesRepresentativeRepository.findByCode("REP1")).thenReturn(Optional.of(entity));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(salesRepresentativeRepository.save(entity)).thenReturn(entity);

        salesRepresentativeService.update(id, request());

        verify(userMapper).updateEntity(request(), entity);
        assertThat(entity.getUser()).isSameAs(user);
    }

    @Test
    void setActiveTogglesFlag() {
        UUID id = UUID.randomUUID();
        SalesRepresentativeEntity entity = new SalesRepresentativeEntity();
        entity.setActive(true);
        when(salesRepresentativeRepository.findById(id)).thenReturn(Optional.of(entity));
        when(salesRepresentativeRepository.save(entity)).thenReturn(entity);

        salesRepresentativeService.setActive(id, false);

        assertThat(entity.isActive()).isFalse();
    }
}
