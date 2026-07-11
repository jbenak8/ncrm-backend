package cz.jbenak.ncrm_backend.services;

import cz.jbenak.ncrm_backend.model.dto.customer.MeetingDto;
import cz.jbenak.ncrm_backend.model.dto.customer.MeetingRequest;
import cz.jbenak.ncrm_backend.model.entity.customer.MeetingEntity;
import cz.jbenak.ncrm_backend.model.mapper.MeetingMapper;
import cz.jbenak.ncrm_backend.repository.ContactPersonRepository;
import cz.jbenak.ncrm_backend.repository.CustomerRepository;
import cz.jbenak.ncrm_backend.repository.CustomerSiteRepository;
import cz.jbenak.ncrm_backend.repository.MeetingRepository;
import cz.jbenak.ncrm_backend.repository.SalesRepresentativeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-11
 * Service for planning and realization of business meetings including writing the meeting minutes (outcome).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final CustomerRepository customerRepository;
    private final ContactPersonRepository contactPersonRepository;
    private final CustomerSiteRepository customerSiteRepository;
    private final SalesRepresentativeRepository salesRepresentativeRepository;
    private final MeetingMapper meetingMapper;

    @Transactional(readOnly = true)
    public List<MeetingDto> findAll() {
        return meetingMapper.toDtoList(meetingRepository.findAll());
    }

    @Transactional(readOnly = true)
    public MeetingDto findById(UUID id) {
        return meetingMapper.toDto(getMeeting(id));
    }

    @Transactional(readOnly = true)
    public List<MeetingDto> findByCustomer(UUID customerId) {
        return meetingMapper.toDtoList(meetingRepository.findAllByCustomerId(customerId));
    }

    @Transactional(readOnly = true)
    public List<MeetingDto> findBySalesRepresentative(UUID salesRepresentativeId) {
        return meetingMapper.toDtoList(meetingRepository.findAllBySalesRepresentativeId(salesRepresentativeId));
    }

    public MeetingDto create(MeetingRequest request) {
        MeetingEntity entity = new MeetingEntity();
        applyRequest(entity, request);
        if (entity.getStatus() == null) {
            entity.setStatus(MeetingEntity.MeetingStatus.PLANNED);
        }
        log.info("Planning meeting '{}' for customer {} on {}", request.subject(), request.customerId(), request.plannedDate());
        return meetingMapper.toDto(meetingRepository.save(entity));
    }

    public MeetingDto update(UUID id, MeetingRequest request) {
        MeetingEntity entity = getMeeting(id);
        applyRequest(entity, request);
        return meetingMapper.toDto(meetingRepository.save(entity));
    }

    /**
     * Completes the meeting and stores the meeting minutes (outcome).
     */
    public MeetingDto complete(UUID id, String outcome) {
        MeetingEntity entity = getMeeting(id);
        entity.setStatus(MeetingEntity.MeetingStatus.COMPLETED);
        entity.setActualDate(LocalDateTime.now(ZoneId.systemDefault()));
        entity.setOutcome(outcome);
        log.info("Completed meeting {} and stored the meeting minutes", id);
        return meetingMapper.toDto(meetingRepository.save(entity));
    }

    public MeetingDto cancel(UUID id) {
        MeetingEntity entity = getMeeting(id);
        entity.setStatus(MeetingEntity.MeetingStatus.CANCELLED);
        log.info("Cancelled meeting {}", id);
        return meetingMapper.toDto(meetingRepository.save(entity));
    }

    private MeetingEntity getMeeting(UUID id) {
        return meetingRepository.findById(id).orElseThrow(() -> new NotFoundException("Meeting", id));
    }

    private void applyRequest(MeetingEntity entity, MeetingRequest request) {
        entity.setCustomer(customerRepository.findById(request.customerId())
                .orElseThrow(() -> new NotFoundException("Customer", request.customerId())));
        entity.setSalesRepresentative(salesRepresentativeRepository.findById(request.salesRepresentativeId())
                .orElseThrow(() -> new NotFoundException("SalesRepresentative", request.salesRepresentativeId())));
        entity.setContactPerson(request.contactPersonId() == null ? null
                : contactPersonRepository.findById(request.contactPersonId())
                .orElseThrow(() -> new NotFoundException("ContactPerson", request.contactPersonId())));
        entity.setCustomerSite(request.customerSiteId() == null ? null
                : customerSiteRepository.findById(request.customerSiteId())
                .orElseThrow(() -> new NotFoundException("CustomerSite", request.customerSiteId())));
        entity.setSubject(request.subject());
        entity.setDescription(request.description());
        entity.setPlannedDate(request.plannedDate());
        entity.setActualDate(request.actualDate());
        if (request.status() != null) {
            entity.setStatus(request.status());
        }
        entity.setOutcome(request.outcome());
    }
}
