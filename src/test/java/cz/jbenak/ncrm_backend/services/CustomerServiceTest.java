package cz.jbenak.ncrm_backend.services;

import cz.jbenak.ncrm_backend.model.dto.AddressDto;
import cz.jbenak.ncrm_backend.model.dto.customer.CustomerRequest;
import cz.jbenak.ncrm_backend.model.entity.AddressEntity;
import cz.jbenak.ncrm_backend.model.entity.CountryEntity;
import cz.jbenak.ncrm_backend.model.entity.company.SalesRepresentativeEntity;
import cz.jbenak.ncrm_backend.model.entity.customer.CustomerEntity;
import cz.jbenak.ncrm_backend.model.mapper.AddressMapper;
import cz.jbenak.ncrm_backend.model.mapper.CustomerMapper;
import cz.jbenak.ncrm_backend.repository.CountryRepository;
import cz.jbenak.ncrm_backend.repository.CustomerRepository;
import cz.jbenak.ncrm_backend.repository.SalesRepresentativeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests of {@link CustomerService} covering CRUD operations, reference resolution
 * (sales representative, country) and error handling.
 */
@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private SalesRepresentativeRepository salesRepresentativeRepository;
    @Mock
    private CountryRepository countryRepository;
    @Mock
    private CustomerMapper customerMapper;
    @Mock
    private AddressMapper addressMapper;

    @InjectMocks
    private CustomerService customerService;

    private CustomerRequest request(UUID repId, AddressDto address) {
        return new CustomerRequest(null, "ACME s.r.o.", "12345678", "CZ12345678",
                "info@acme.cz", "+420123456789", address, repId, true, null);
    }

    @Test
    void findAllWithoutNameUsesFindAll() {
        Page<CustomerEntity> page = new PageImpl<>(List.of(new CustomerEntity()));
        when(customerRepository.findAll(any(Pageable.class))).thenReturn(page);

        customerService.findAll(null, Pageable.unpaged());

        verify(customerRepository).findAll(any(Pageable.class));
        verify(customerRepository, never()).findAllByNameContainingIgnoreCase(any(), any());
    }

    @Test
    void findAllWithNameUsesNameFilter() {
        when(customerRepository.findAllByNameContainingIgnoreCase(any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        customerService.findAll("acme", Pageable.unpaged());

        verify(customerRepository).findAllByNameContainingIgnoreCase(any(), any());
    }

    @Test
    void findByIdThrowsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(customerRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.findById(id))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Customer");
    }

    @Test
    void findBySalesRepresentativeDelegatesToRepository() {
        UUID repId = UUID.randomUUID();
        when(customerRepository.findAllBySalesRepresentativeId(repId)).thenReturn(List.of());

        customerService.findBySalesRepresentative(repId);

        verify(customerRepository).findAllBySalesRepresentativeId(repId);
    }

    @Test
    void createRejectsDuplicateRegistrationId() {
        when(customerRepository.existsByRegistrationId("12345678")).thenReturn(true);

        assertThatThrownBy(() -> customerService.create(request(null, null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already exists");
        verify(customerRepository, never()).save(any());
    }

    @Test
    void createResolvesSalesRepresentativeAndCountry() {
        UUID repId = UUID.randomUUID();
        AddressDto address = new AddressDto(null, "Ulice", "1", "Praha", "11000", "CZ", "Česko");
        CustomerRequest request = request(repId, address);
        CustomerEntity entity = new CustomerEntity();
        SalesRepresentativeEntity rep = new SalesRepresentativeEntity();
        AddressEntity addressEntity = new AddressEntity();
        CountryEntity country = new CountryEntity();

        when(customerRepository.existsByRegistrationId("12345678")).thenReturn(false);
        when(customerMapper.toEntity(request)).thenReturn(entity);
        when(salesRepresentativeRepository.findById(repId)).thenReturn(Optional.of(rep));
        when(addressMapper.toEntity(address)).thenReturn(addressEntity);
        when(countryRepository.findById("CZ")).thenReturn(Optional.of(country));
        when(customerRepository.save(entity)).thenReturn(entity);

        customerService.create(request);

        assertThat(entity.getSalesRepresentative()).isSameAs(rep);
        assertThat(entity.getHeadquartersAddress()).isSameAs(addressEntity);
        assertThat(addressEntity.getCountry()).isSameAs(country);
        verify(customerRepository).save(entity);
    }

    @Test
    void createThrowsWhenSalesRepresentativeMissing() {
        UUID repId = UUID.randomUUID();
        when(customerRepository.existsByRegistrationId("12345678")).thenReturn(false);
        when(customerMapper.toEntity(any(CustomerRequest.class))).thenReturn(new CustomerEntity());
        when(salesRepresentativeRepository.findById(repId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.create(request(repId, null)))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("SalesRepresentative");
    }

    @Test
    void updateClearsSalesRepresentativeWhenNotProvided() {
        UUID id = UUID.randomUUID();
        CustomerEntity entity = new CustomerEntity();
        entity.setSalesRepresentative(new SalesRepresentativeEntity());
        when(customerRepository.findById(id)).thenReturn(Optional.of(entity));
        when(customerRepository.save(entity)).thenReturn(entity);

        customerService.update(id, request(null, null));

        assertThat(entity.getSalesRepresentative()).isNull();
        verify(customerMapper).updateEntity(any(CustomerRequest.class), any(CustomerEntity.class));
    }

    @Test
    void deactivateSetsActiveFalse() {
        UUID id = UUID.randomUUID();
        CustomerEntity entity = new CustomerEntity();
        entity.setActive(true);
        when(customerRepository.findById(id)).thenReturn(Optional.of(entity));

        customerService.deactivate(id);

        assertThat(entity.isActive()).isFalse();
        verify(customerRepository).save(entity);
    }
}
