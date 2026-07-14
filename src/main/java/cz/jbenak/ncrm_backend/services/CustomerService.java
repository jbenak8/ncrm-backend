package cz.jbenak.ncrm_backend.services;

import cz.jbenak.ncrm_backend.model.dto.customer.CustomerDto;
import cz.jbenak.ncrm_backend.model.dto.customer.CustomerRequest;
import cz.jbenak.ncrm_backend.model.entity.AddressEntity;
import cz.jbenak.ncrm_backend.model.entity.customer.CustomerEntity;
import cz.jbenak.ncrm_backend.model.mapper.AddressMapper;
import cz.jbenak.ncrm_backend.model.mapper.CustomerMapper;
import cz.jbenak.ncrm_backend.repository.CountryRepository;
import cz.jbenak.ncrm_backend.repository.CustomerRepository;
import cz.jbenak.ncrm_backend.repository.SalesRepresentativeRepository;
import cz.jbenak.ncrm_backend.search.SearchSpecificationBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-11
 * Service providing CRUD operations for customers, including resolution of the sales representative
 * and the country of the headquarters address. Customer data can be pre-filled from ARES on the frontend.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CustomerService {

    /** Attribute paths of {@link CustomerEntity} that can be used by the generic search API. */
    private static final Set<String> SEARCHABLE_FIELDS = Set.of(
            "designation", "name", "registrationId", "vatId", "email", "phone", "active", "note",
            "headquartersAddress.city", "headquartersAddress.zipCode", "salesRepresentative.id");

    private final CustomerRepository customerRepository;
    private final SalesRepresentativeRepository salesRepresentativeRepository;
    private final CountryRepository countryRepository;
    private final CustomerMapper customerMapper;
    private final AddressMapper addressMapper;

    @Transactional(readOnly = true)
    public Page<CustomerDto> findAll(String name, Pageable pageable) {
        Page<CustomerEntity> page = (name == null || name.isBlank())
                ? customerRepository.findAll(pageable)
                : customerRepository.findAllByNameContainingIgnoreCase(name, pageable);
        return page.map(customerMapper::toDto);
    }

    /**
     * Generic search over customers. Filters are raw {@code field:operator:value} expressions,
     * combined with a logical AND; see {@link SearchSpecificationBuilder}.
     */
    @Transactional(readOnly = true)
    public Page<CustomerDto> search(List<String> filters, Pageable pageable) {
        log.debug("Searching customers with filters {}", filters);
        return customerRepository.findAll(SearchSpecificationBuilder.build(filters, SEARCHABLE_FIELDS), pageable)
                .map(customerMapper::toDto);
    }

    @Transactional(readOnly = true)
    public CustomerDto findById(UUID id) {
        return customerMapper.toDto(getCustomer(id));
    }

    @Transactional(readOnly = true)
    public List<CustomerDto> findBySalesRepresentative(UUID salesRepresentativeId) {
        return customerMapper.toDtoList(customerRepository.findAllBySalesRepresentativeId(salesRepresentativeId));
    }

    public CustomerDto create(CustomerRequest request) {
        if (customerRepository.existsByRegistrationId(request.registrationId())) {
            throw new IllegalStateException("Customer with registration id " + request.registrationId() + " already exists");
        }
        CustomerEntity entity = customerMapper.toEntity(request);
        applyReferences(entity, request);
        CustomerEntity saved = customerRepository.save(entity);
        log.info("Created customer '{}' (registration id {}) with id {}", request.name(), request.registrationId(), saved.getId());
        return customerMapper.toDto(saved);
    }

    public CustomerDto update(UUID id, CustomerRequest request) {
        CustomerEntity entity = getCustomer(id);
        customerMapper.updateEntity(request, entity);
        applyReferences(entity, request);
        log.info("Updating customer {} ({})", id, request.name());
        return customerMapper.toDto(customerRepository.save(entity));
    }

    public void deactivate(UUID id) {
        CustomerEntity entity = getCustomer(id);
        entity.setActive(false);
        customerRepository.save(entity);
        log.info("Deactivated customer {} ({})", id, entity.getName());
    }

    private CustomerEntity getCustomer(UUID id) {
        return customerRepository.findById(id).orElseThrow(() -> new NotFoundException("Customer", id));
    }

    private void applyReferences(CustomerEntity entity, CustomerRequest request) {
        if (request.salesRepresentativeId() != null) {
            entity.setSalesRepresentative(salesRepresentativeRepository.findById(request.salesRepresentativeId())
                    .orElseThrow(() -> new NotFoundException("SalesRepresentative", request.salesRepresentativeId())));
        } else {
            entity.setSalesRepresentative(null);
        }
        if (request.headquartersAddress() != null) {
            AddressEntity address = addressMapper.toEntity(request.headquartersAddress());
            if (request.headquartersAddress().countryIsoCode() != null) {
                address.setCountry(countryRepository.findById(request.headquartersAddress().countryIsoCode())
                        .orElseThrow(() -> new NotFoundException("Country " + request.headquartersAddress().countryIsoCode() + " not found")));
            }
            entity.setHeadquartersAddress(address);
        }
    }
}
