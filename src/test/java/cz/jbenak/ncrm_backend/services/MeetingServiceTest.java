package cz.jbenak.ncrm_backend.services;

import cz.jbenak.ncrm_backend.model.dto.customer.MeetingRequest;
import cz.jbenak.ncrm_backend.model.entity.company.SalesRepresentativeEntity;
import cz.jbenak.ncrm_backend.model.entity.customer.CustomerEntity;
import cz.jbenak.ncrm_backend.model.entity.customer.MeetingEntity;
import cz.jbenak.ncrm_backend.model.mapper.MeetingMapper;
import cz.jbenak.ncrm_backend.repository.ContactPersonRepository;
import cz.jbenak.ncrm_backend.repository.CustomerRepository;
import cz.jbenak.ncrm_backend.repository.CustomerSiteRepository;
import cz.jbenak.ncrm_backend.repository.MeetingRepository;
import cz.jbenak.ncrm_backend.repository.SalesRepresentativeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests of {@link MeetingService} covering planning, updating, completion (meeting minutes)
 * and cancellation of business meetings.
 */
@ExtendWith(MockitoExtension.class)
class MeetingServiceTest {

    @Mock
    private MeetingRepository meetingRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private ContactPersonRepository contactPersonRepository;
    @Mock
    private CustomerSiteRepository customerSiteRepository;
    @Mock
    private SalesRepresentativeRepository salesRepresentativeRepository;
    @Mock
    private MeetingMapper meetingMapper;
    @Mock
    private MeetingEmailService meetingEmailService;

    @InjectMocks
    private MeetingService meetingService;

    private final UUID customerId = UUID.randomUUID();
    private final UUID repId = UUID.randomUUID();

    private MeetingRequest request(MeetingEntity.MeetingStatus status) {
        return new MeetingRequest(customerId, null, repId, null, "Introduction",
                "First meeting", LocalDateTime.of(2026, Month.JULY, 20, 10, 0), null, status, null);
    }

    @Test
    void findAllDelegatesToRepository() {
        when(meetingRepository.findAll()).thenReturn(List.of());

        meetingService.findAll();

        verify(meetingMapper).toDtoList(List.of());
    }

    @Test
    void findByIdThrowsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(meetingRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> meetingService.findById(id))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void findByCustomerAndRepresentativeDelegateToRepository() {
        when(meetingRepository.findAllByCustomerId(customerId)).thenReturn(List.of());
        when(meetingRepository.findAllBySalesRepresentativeId(repId)).thenReturn(List.of());

        meetingService.findByCustomer(customerId);
        meetingService.findBySalesRepresentative(repId);

        verify(meetingRepository).findAllByCustomerId(customerId);
        verify(meetingRepository).findAllBySalesRepresentativeId(repId);
    }

    @Test
    void createDefaultsToPlannedStatus() {
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(new CustomerEntity()));
        when(salesRepresentativeRepository.findById(repId)).thenReturn(Optional.of(new SalesRepresentativeEntity()));
        when(meetingRepository.save(any(MeetingEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        meetingService.create(request(null));

        ArgumentCaptor<MeetingEntity> captor = ArgumentCaptor.forClass(MeetingEntity.class);
        verify(meetingRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(MeetingEntity.MeetingStatus.PLANNED);
        assertThat(captor.getValue().getSubject()).isEqualTo("Introduction");
        verify(meetingEmailService).sendMeetingCreated(captor.getValue());
    }

    @Test
    void createThrowsWhenCustomerMissing() {
        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

        MeetingRequest request = request(null);
        assertThatThrownBy(() -> meetingService.create(request))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Customer");
    }

    @Test
    void updateAppliesRequestedStatus() {
        UUID id = UUID.randomUUID();
        MeetingEntity entity = new MeetingEntity();
        when(meetingRepository.findById(id)).thenReturn(Optional.of(entity));
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(new CustomerEntity()));
        when(salesRepresentativeRepository.findById(repId)).thenReturn(Optional.of(new SalesRepresentativeEntity()));
        when(meetingRepository.save(entity)).thenReturn(entity);

        meetingService.update(id, request(MeetingEntity.MeetingStatus.COMPLETED));

        assertThat(entity.getStatus()).isEqualTo(MeetingEntity.MeetingStatus.COMPLETED);
        verify(meetingEmailService).sendMeetingUpdated(entity);
    }

    @Test
    void completeStoresOutcomeAndActualDate() {
        UUID id = UUID.randomUUID();
        MeetingEntity entity = new MeetingEntity();
        when(meetingRepository.findById(id)).thenReturn(Optional.of(entity));
        when(meetingRepository.save(entity)).thenReturn(entity);

        meetingService.complete(id, "Order agreed");

        assertThat(entity.getStatus()).isEqualTo(MeetingEntity.MeetingStatus.COMPLETED);
        assertThat(entity.getOutcome()).isEqualTo("Order agreed");
        assertThat(entity.getActualDate()).isNotNull();
    }

    @Test
    void cancelSetsCancelledStatus() {
        UUID id = UUID.randomUUID();
        MeetingEntity entity = new MeetingEntity();
        when(meetingRepository.findById(id)).thenReturn(Optional.of(entity));
        when(meetingRepository.save(entity)).thenReturn(entity);

        meetingService.cancel(id);

        assertThat(entity.getStatus()).isEqualTo(MeetingEntity.MeetingStatus.CANCELLED);
    }
}
