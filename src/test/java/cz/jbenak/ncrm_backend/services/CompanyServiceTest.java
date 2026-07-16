package cz.jbenak.ncrm_backend.services;

import cz.jbenak.ncrm_backend.model.dto.AddressDto;
import cz.jbenak.ncrm_backend.model.dto.company.CompanyRequest;
import cz.jbenak.ncrm_backend.model.entity.AddressEntity;
import cz.jbenak.ncrm_backend.model.entity.CountryEntity;
import cz.jbenak.ncrm_backend.model.entity.company.CompanyEntity;
import cz.jbenak.ncrm_backend.model.mapper.AddressMapper;
import cz.jbenak.ncrm_backend.model.mapper.CompanyMapper;
import cz.jbenak.ncrm_backend.repository.CompanyRepository;
import cz.jbenak.ncrm_backend.repository.CountryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests of {@link CompanyService} covering listing, creation with duplicate protection,
 * address country resolution, soft deletion and management of the default company.
 */
@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    @Mock
    private CompanyRepository companyRepository;
    @Mock
    private CountryRepository countryRepository;
    @Mock
    private CompanyMapper companyMapper;
    @Mock
    private AddressMapper addressMapper;

    @InjectMocks
    private CompanyService companyService;

    private CompanyRequest request(AddressDto address, boolean defaultCompany) {
        return new CompanyRequest("ACME s.r.o.", null, "12345678", "CZ12345678", address,
                null, null, "+420123456789", "info@acme.cz", null, null, null, null, null,
                true, defaultCompany);
    }

    @Test
    void findAllListsOnlyNotDeleted() {
        when(companyRepository.findAllByDeletedFalse()).thenReturn(java.util.List.of());

        companyService.findAll();

        verify(companyRepository).findAllByDeletedFalse();
    }

    @Test
    void findByIdThrowsForDeletedCompany() {
        UUID id = UUID.randomUUID();
        CompanyEntity deleted = new CompanyEntity();
        deleted.setDeleted(true);
        when(companyRepository.findById(id)).thenReturn(Optional.of(deleted));

        assertThatThrownBy(() -> companyService.findById(id))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void createRejectsDuplicateRegistrationId() {
        when(companyRepository.existsByRegistrationId("12345678")).thenReturn(true);

        CompanyRequest request = request(null, false);
        assertThatThrownBy(() -> companyService.create(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("12345678");
    }

    @Test
    void createResolvesAddressCountry() {
        AddressDto addressDto = new AddressDto(null, "Ulice", "1", null, "Praha", "11000", "CZE", null);
        CompanyEntity entity = new CompanyEntity();
        AddressEntity address = new AddressEntity();
        CountryEntity country = new CountryEntity();
        when(companyRepository.existsByRegistrationId("12345678")).thenReturn(false);
        when(companyMapper.toEntity(any(CompanyRequest.class))).thenReturn(entity);
        when(addressMapper.toEntity(addressDto)).thenReturn(address);
        when(countryRepository.findById("CZE")).thenReturn(Optional.of(country));
        when(companyRepository.save(entity)).thenReturn(entity);

        companyService.create(request(addressDto, false));

        assertThat(entity.getAddress()).isSameAs(address);
        assertThat(address.getCountry()).isSameAs(country);
    }

    @Test
    void createAsDefaultClearsPreviousDefault() {
        CompanyEntity entity = new CompanyEntity();
        CompanyEntity previousDefault = new CompanyEntity();
        previousDefault.setDefaultCompany(true);
        when(companyRepository.existsByRegistrationId("12345678")).thenReturn(false);
        when(companyMapper.toEntity(any(CompanyRequest.class))).thenReturn(entity);
        when(companyRepository.findByDefaultCompanyTrueAndDeletedFalse()).thenReturn(Optional.of(previousDefault));
        when(companyRepository.save(any(CompanyEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        companyService.create(request(null, true));

        assertThat(previousDefault.isDefaultCompany()).isFalse();
    }

    @Test
    void deleteMarksCompanyAsSoftDeleted() {
        UUID id = UUID.randomUUID();
        CompanyEntity entity = new CompanyEntity();
        entity.setDefaultCompany(true);
        when(companyRepository.findById(id)).thenReturn(Optional.of(entity));
        when(companyRepository.save(entity)).thenReturn(entity);

        companyService.delete(id, "owner");

        assertThat(entity.isDeleted()).isTrue();
        assertThat(entity.isActive()).isFalse();
        assertThat(entity.isDefaultCompany()).isFalse();
        assertThat(entity.getDeletedAt()).isNotNull();
        assertThat(entity.getDeletedBy()).isEqualTo("owner");
    }

    @Test
    void uploadLogoStoresContentAndContentType() {
        UUID id = UUID.randomUUID();
        CompanyEntity entity = new CompanyEntity();
        MockMultipartFile file = new MockMultipartFile("file", "logo.png", "image/png", new byte[]{1, 2, 3});
        when(companyRepository.findById(id)).thenReturn(Optional.of(entity));
        when(companyRepository.save(entity)).thenReturn(entity);

        companyService.uploadLogo(id, file);

        assertThat(entity.getLogo()).containsExactly(1, 2, 3);
        assertThat(entity.getLogoContentType()).isEqualTo("image/png");
    }

    @Test
    void uploadLogoRejectsEmptyFile() {
        UUID id = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "logo.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> companyService.uploadLogo(id, file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void uploadLogoRejectsUnsupportedContentType() {
        UUID id = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "logo.pdf", "application/pdf", new byte[]{1});

        assertThatThrownBy(() -> companyService.uploadLogo(id, file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("application/pdf");
    }

    @Test
    void uploadLogoRejectsOversizedFile() {
        UUID id = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "logo.png", "image/png", new byte[2 * 1024 * 1024 + 1]);

        assertThatThrownBy(() -> companyService.uploadLogo(id, file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("size");
    }

    @Test
    void getLogoThrowsWhenCompanyHasNoLogo() {
        UUID id = UUID.randomUUID();
        when(companyRepository.findById(id)).thenReturn(Optional.of(new CompanyEntity()));

        assertThatThrownBy(() -> companyService.getLogo(id))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("logo");
    }

    @Test
    void getLogoReturnsStoredContent() {
        UUID id = UUID.randomUUID();
        CompanyEntity entity = new CompanyEntity();
        entity.setLogo(new byte[]{1, 2, 3});
        entity.setLogoContentType("image/png");
        when(companyRepository.findById(id)).thenReturn(Optional.of(entity));

        var logo = companyService.getLogo(id);

        assertThat(logo.content()).containsExactly(1, 2, 3);
        assertThat(logo.contentType()).isEqualTo("image/png");
    }

    @Test
    void deleteLogoClearsContentAndContentType() {
        UUID id = UUID.randomUUID();
        CompanyEntity entity = new CompanyEntity();
        entity.setLogo(new byte[]{1, 2, 3});
        entity.setLogoContentType("image/png");
        when(companyRepository.findById(id)).thenReturn(Optional.of(entity));
        when(companyRepository.save(entity)).thenReturn(entity);

        companyService.deleteLogo(id);

        assertThat(entity.getLogo()).isNull();
        assertThat(entity.getLogoContentType()).isNull();
    }

    @Test
    void setDefaultMovesFlagToRequestedCompany() {
        UUID id = UUID.randomUUID();
        CompanyEntity entity = new CompanyEntity();
        CompanyEntity previousDefault = new CompanyEntity();
        previousDefault.setDefaultCompany(true);
        when(companyRepository.findById(id)).thenReturn(Optional.of(entity));
        when(companyRepository.findByDefaultCompanyTrueAndDeletedFalse()).thenReturn(Optional.of(previousDefault));
        when(companyRepository.save(any(CompanyEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        companyService.setDefault(id);

        assertThat(entity.isDefaultCompany()).isTrue();
        assertThat(previousDefault.isDefaultCompany()).isFalse();
    }
}
