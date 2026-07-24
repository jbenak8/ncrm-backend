package cz.jbenak.ncrm_backend.services;

import cz.jbenak.ncrm_backend.model.dto.company.CompanyDto;
import cz.jbenak.ncrm_backend.model.dto.company.CompanyLogoDto;
import cz.jbenak.ncrm_backend.model.dto.company.CompanyRequest;
import cz.jbenak.ncrm_backend.model.entity.AddressEntity;
import cz.jbenak.ncrm_backend.model.entity.company.CompanyEntity;
import cz.jbenak.ncrm_backend.model.mapper.AddressMapper;
import cz.jbenak.ncrm_backend.model.mapper.CompanyMapper;
import cz.jbenak.ncrm_backend.repository.CompanyRepository;
import cz.jbenak.ncrm_backend.repository.CountryRepository;
import cz.jbenak.ncrm_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-13
 * Service for administration of own companies by the owner. Companies are only soft-deleted
 * (deletion metadata is recorded) and at most one company can be marked as the default one.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CompanyService {

    private static final Set<String> ALLOWED_LOGO_CONTENT_TYPES =
            Set.of("image/png", "image/jpeg", "image/gif", "image/webp", "image/svg+xml");
    private static final long MAX_LOGO_SIZE_BYTES = 2 * 1024L * 1024;

    private final CompanyRepository companyRepository;
    private final CountryRepository countryRepository;
    private final UserRepository userRepository;
    private final CompanyMapper companyMapper;
    private final AddressMapper addressMapper;

    @Transactional(readOnly = true)
    public List<CompanyDto> findAll() {
        return companyMapper.toDtoList(companyRepository.findAllByDeletedFalse());
    }

    /**
     * Lists the companies visible to the given user. Administrators (and users without an
     * explicit assignment) see all companies, other users only the companies they are assigned to.
     */
    @Transactional(readOnly = true)
    public List<CompanyDto> findAllForUser(String username, boolean unrestricted) {
        if (unrestricted || username == null) {
            return findAll();
        }
        return userRepository.findByUsername(username)
                .map(user -> user.getCompanies().stream()
                        .filter(company -> !company.isDeleted())
                        .sorted(Comparator.comparing(CompanyEntity::getName, String.CASE_INSENSITIVE_ORDER))
                        .toList())
                .filter(companies -> !companies.isEmpty())
                .map(companyMapper::toDtoList)
                .orElseGet(this::findAll);
    }

    /**
     * Lists strictly the companies assigned to the given user, without any fallback to the
     * full list. Used for customer accounts which may only see their assigned companies.
     */
    @Transactional(readOnly = true)
    public List<CompanyDto> findAssignedTo(String username) {
        return userRepository.findByUsername(username)
                .map(user -> user.getCompanies().stream()
                        .filter(company -> !company.isDeleted())
                        .sorted(Comparator.comparing(CompanyEntity::getName, String.CASE_INSENSITIVE_ORDER))
                        .toList())
                .map(companyMapper::toDtoList)
                .orElseGet(List::of);
    }

    @Transactional(readOnly = true)
    public CompanyDto findById(UUID id) {
        return companyMapper.toDto(getCompany(id));
    }

    public CompanyDto create(CompanyRequest request) {
        if (companyRepository.existsByRegistrationId(request.registrationId())) {
            throw new IllegalStateException("Company with registration id " + request.registrationId() + " already exists");
        }
        CompanyEntity entity = companyMapper.toEntity(request);
        applyAddress(entity, request);
        if (request.defaultCompany()) {
            clearDefaultCompany();
        }
        CompanyEntity saved = companyRepository.save(entity);
        log.info("Created company '{}' (registration id {}) with id {}", request.name(), request.registrationId(), saved.getId());
        return companyMapper.toDto(saved);
    }

    public CompanyDto update(UUID id, CompanyRequest request) {
        CompanyEntity entity = getCompany(id);
        if (request.defaultCompany() && !entity.isDefaultCompany()) {
            clearDefaultCompany();
        }
        companyMapper.updateEntity(request, entity);
        applyAddress(entity, request);
        log.info("Updating company {} ({})", id, request.name());
        return companyMapper.toDto(companyRepository.save(entity));
    }

    public void delete(UUID id, String deletedBy) {
        CompanyEntity entity = getCompany(id);
        entity.setDeleted(true);
        entity.setActive(false);
        entity.setDefaultCompany(false);
        entity.setDeletedAt(LocalDate.now(ZoneId.systemDefault()));
        entity.setDeletedBy(deletedBy);
        companyRepository.save(entity);
        log.info("Soft-deleted company {} ({}) by {}", id, entity.getName(), deletedBy);
    }

    public CompanyDto setDefault(UUID id) {
        CompanyEntity entity = getCompany(id);
        clearDefaultCompany();
        entity.setDefaultCompany(true);
        log.info("Company {} ({}) is now the default company", id, entity.getName());
        return companyMapper.toDto(companyRepository.save(entity));
    }

    public CompanyDto uploadLogo(UUID id, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Logo file must not be empty");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_LOGO_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Unsupported logo content type " + contentType
                    + ". Allowed types are: " + String.join(", ", ALLOWED_LOGO_CONTENT_TYPES));
        }
        if (file.getSize() > MAX_LOGO_SIZE_BYTES) {
            throw new IllegalArgumentException("Logo file exceeds the maximum allowed size of " + MAX_LOGO_SIZE_BYTES + " bytes");
        }
        CompanyEntity entity = getCompany(id);
        try {
            entity.setLogo(file.getBytes());
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read the uploaded logo file", e);
        }
        entity.setLogoContentType(contentType);
        log.info("Uploaded logo ({}, {} bytes) for company {} ({})", contentType, file.getSize(), id, entity.getName());
        return companyMapper.toDto(companyRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public CompanyLogoDto getLogo(UUID id) {
        CompanyEntity entity = getCompany(id);
        if (entity.getLogo() == null) {
            throw new NotFoundException("Company " + id + " has no logo");
        }
        return new CompanyLogoDto(entity.getLogo(), entity.getLogoContentType());
    }

    public CompanyDto deleteLogo(UUID id) {
        CompanyEntity entity = getCompany(id);
        entity.setLogo(null);
        entity.setLogoContentType(null);
        log.info("Deleted logo of company {} ({})", id, entity.getName());
        return companyMapper.toDto(companyRepository.save(entity));
    }

    private CompanyEntity getCompany(UUID id) {
        return companyRepository.findById(id)
                .filter(company -> !company.isDeleted())
                .orElseThrow(() -> new NotFoundException("Company", id));
    }

    private void clearDefaultCompany() {
        companyRepository.findByDefaultCompanyTrueAndDeletedFalse().ifPresent(current -> {
            current.setDefaultCompany(false);
            companyRepository.save(current);
        });
    }

    private void applyAddress(CompanyEntity entity, CompanyRequest request) {
        if (request.address() != null) {
            AddressEntity address = addressMapper.toEntity(request.address());
            if (request.address().countryIsoCode() != null) {
                address.setCountry(countryRepository.findById(request.address().countryIsoCode())
                        .orElseThrow(() -> new NotFoundException("Country " + request.address().countryIsoCode() + " not found")));
            }
            entity.setAddress(address);
        }
    }
}
