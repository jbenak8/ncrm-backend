package cz.jbenak.ncrm_backend.services;

import cz.jbenak.ncrm_backend.model.dto.company.SalesRepresentativeDto;
import cz.jbenak.ncrm_backend.model.dto.security.ChangePasswordRequest;
import cz.jbenak.ncrm_backend.model.dto.security.RoleDto;
import cz.jbenak.ncrm_backend.model.dto.security.UserDto;
import cz.jbenak.ncrm_backend.model.dto.security.UserRequest;
import cz.jbenak.ncrm_backend.model.entity.company.CompanyEntity;
import cz.jbenak.ncrm_backend.model.entity.customer.CustomerEntity;
import cz.jbenak.ncrm_backend.model.entity.security.RoleEntity;
import cz.jbenak.ncrm_backend.model.entity.security.UserEntity;
import cz.jbenak.ncrm_backend.model.mapper.UserMapper;
import cz.jbenak.ncrm_backend.repository.CompanyRepository;
import cz.jbenak.ncrm_backend.repository.CustomerRepository;
import cz.jbenak.ncrm_backend.repository.RoleRepository;
import cz.jbenak.ncrm_backend.repository.SalesRepresentativeRepository;
import cz.jbenak.ncrm_backend.repository.UserRepository;
import cz.jbenak.ncrm_backend.search.SearchSpecificationBuilder;
import cz.jbenak.ncrm_backend.security.PasswordPolicy;
import jakarta.mail.internet.InternetAddress;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-11
 * Service for user administration. Authentication itself is delegated to Keycloak (OIDC);
 * this service only manages the local user projection and account state flags.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    @Value("${spring.mail.properties.mail.from.name}")
    private String mailFromName;

    @Value("${spring.mail.properties.mail.from.address}")
    private String mailFromAddress;

    /** Attribute paths of the user entity that can be used by the generic search API. */
    private static final Set<String> SEARCHABLE_FIELDS = Set.of(
            "username", "email", "firstName", "lastName", "enabled", "locked",
            "credentialsExpired", "mustChangePassword", "lastLoginAt");
    private static final String NOT_FOUND_TEXT = " not found.";
    /** Name of the role that allows linking the user account to a customer. */
    private static final String CUSTOMER_ROLE = "CUSTOMER";

    private final UserRepository userRepository;
    private final SalesRepresentativeRepository salesRepresentativeRepository;
    private final RoleRepository roleRepository;
    private final CompanyRepository companyRepository;
    private final CustomerRepository customerRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;

    @Transactional(readOnly = true)
    public List<UserDto> findAll() {
        return userMapper.toDtoList(userRepository.findAll());
    }

    /**
     * Generic search over user accounts. Filters are raw {@code field:operator:value} expressions,
     * combined with a logical AND; see {@link SearchSpecificationBuilder}.
     */
    @Transactional(readOnly = true)
    public Page<UserDto> search(List<String> filters, Pageable pageable) {
        log.debug("Searching users with filters {}", filters);
        return userRepository.findAll(SearchSpecificationBuilder.build(filters, SEARCHABLE_FIELDS), pageable)
                .map(userMapper::toDto);
    }

    /** Returns all security roles assignable to user accounts. */
    @Transactional(readOnly = true)
    public List<RoleDto> findAllRoles() {
        return userMapper.toRoleDtoList(roleRepository.findAll());
    }

    @Transactional(readOnly = true)
    public UserDto findByUsername(String username) {
        return userRepository.findByUsername(username).map(userMapper::toDto)
                .orElseThrow(() -> new NotFoundException("User " + username + NOT_FOUND_TEXT));
    }

    @Transactional(readOnly = true)
    public List<SalesRepresentativeDto> findAllSalesRepresentatives() {
        return userMapper.toRepresentativeDtoList(salesRepresentativeRepository.findAllByActiveTrue());
    }

    public void recordLogin(String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            user.setLastLoginAt(LocalDateTime.now(ZoneId.systemDefault()));
            userRepository.save(user);
            log.debug("Recorded login of user {}", username);
        });
    }

    public UserDto create(UserRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new IllegalStateException("User with username " + request.username() + " already exists");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalStateException("User with e-mail " + request.email() + " already exists");
        }
        if (request.password() == null || request.password().isBlank()) {
            throw new IllegalStateException("Password is mandatory when creating a user");
        }
        PasswordPolicy.validate(request.password());
        UserEntity entity = userMapper.toEntity(request);
        entity.setPasswordHash(passwordEncoder.encode(request.password()));
        entity.setRoles(resolveRoles(request.roles()));
        entity.setCompanies(resolveCompanies(request.companyIds()));
        entity.setCustomer(resolveCustomer(request));
        UserEntity saved = userRepository.save(entity);
        log.info("Created user {} with id {}", request.username(), saved.getId());
        if (request.sendCredentials()) {
            sendInitialCredentials(saved, request.password());
        }
        return userMapper.toDto(saved);
    }

    public UserDto update(UUID id, UserRequest request) {
        UserEntity entity = getUser(id);
        userRepository.findByUsername(request.username())
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new IllegalStateException("User with username " + request.username() + " already exists");
                });
        userRepository.findByEmail(request.email())
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new IllegalStateException("User with e-mail " + request.email() + " already exists");
                });
        userMapper.updateEntity(request, entity);
        if (request.password() != null && !request.password().isBlank()) {
            PasswordPolicy.validate(request.password());
            entity.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        entity.setRoles(resolveRoles(request.roles()));
        entity.setCompanies(resolveCompanies(request.companyIds()));
        entity.setCustomer(resolveCustomer(request));
        log.info("Updating user {} ({})", id, request.username());
        return userMapper.toDto(userRepository.save(entity));
    }

    public void delete(UUID id) {
        UserEntity entity = getUser(id);
        salesRepresentativeRepository.findByUserUsername(entity.getUsername()).ifPresent(rep -> {
            throw new IllegalStateException("User " + entity.getUsername()
                    + " is linked to sales representative " + rep.getCode() + " and cannot be deleted");
        });
        userRepository.delete(entity);
        log.info("Deleted user {} ({})", id, entity.getUsername());
    }

    public UserDto setLocked(UUID id, boolean locked) {
        UserEntity user = userRepository.findById(id).orElseThrow(() -> new NotFoundException("User", id));
        user.setLocked(locked);
        log.info("User {} ({}) is now {}", id, user.getUsername(), locked ? "locked" : "unlocked");
        return userMapper.toDto(userRepository.save(user));
    }

    public UserDto setEnabled(UUID id, boolean enabled) {
        UserEntity user = userRepository.findById(id).orElseThrow(() -> new NotFoundException("User", id));
        user.setEnabled(enabled);
        log.info("User {} ({}) is now {}", id, user.getUsername(), enabled ? "enabled" : "disabled");
        return userMapper.toDto(userRepository.save(user));
    }

    /**
     * Changes the password of the given (authenticated) user. The current password must match,
     * the new one must satisfy the password policy. Clears the "must change password" and
     * "credentials expired" flags on success.
     */
    public UserDto changePassword(String username, ChangePasswordRequest request) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User " + username + NOT_FOUND_TEXT));
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is not valid");
        }
        PasswordPolicy.validate(request.newPassword());
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setMustChangePassword(false);
        user.setCredentialsExpired(false);
        log.info("User {} changed their password", username);
        return userMapper.toDto(userRepository.save(user));
    }

    /** Sends the initial login credentials to the newly created user by e-mail. */
    private void sendInitialCredentials(UserEntity user, String rawPassword) {
        try {
            var message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(new InternetAddress(mailFromAddress, mailFromName));
            helper.setTo(user.getEmail());
            helper.setSubject("nCRM – přihlašovací údaje");
            helper.setText("""
                    <p>Dobrý den, %s %s,</p>
                    <p>byl Vám vytvořen účet v systému nCRM. Vaše přihlašovací údaje jsou:</p>
                    <ul><li>Uživatelské jméno: <strong>%s</strong></li>
                    <li>Heslo: <strong>%s</strong></li></ul>
                    <p>Po prvním přihlášení budete vyzváni ke změně hesla.</p>
                    """.formatted(user.getFirstName(), user.getLastName(), user.getUsername(), rawPassword), true);
            mailSender.send(message);
            log.info("Initial credentials sent to {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send initial credentials to {}", user.getEmail(), e);
            throw new IllegalStateException("User was created but the credentials e-mail could not be sent", e);
        }
    }

    private UserEntity getUser(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> new NotFoundException("User", id));
    }

    private Set<RoleEntity> resolveRoles(Set<String> roleNames) {
        return roleNames == null ? new HashSet<>() : roleNames.stream()
                .map(name -> roleRepository.findByName(name)
                        .orElseThrow(() -> new NotFoundException("Role " + name + NOT_FOUND_TEXT)))
                .collect(Collectors.toCollection(HashSet::new));
    }

    /**
     * Resolves the customer the user account is linked to. A customer may be assigned only
     * to users having the {@code CUSTOMER} role; {@code null} means "no customer link".
     */
    private CustomerEntity resolveCustomer(UserRequest request) {
        if (request.customerId() == null) {
            return null;
        }
        if (request.roles() == null || !request.roles().contains(CUSTOMER_ROLE)) {
            throw new IllegalStateException("A customer can only be assigned to a user with the "
                    + CUSTOMER_ROLE + " role");
        }
        return customerRepository.findById(request.customerId())
                .orElseThrow(() -> new NotFoundException("Customer", request.customerId()));
    }

    /** Resolves the own companies the user is assigned to; an empty set means "no restriction". */
    private Set<CompanyEntity> resolveCompanies(Set<UUID> companyIds) {
        return companyIds == null ? new HashSet<>() : companyIds.stream()
                .map(companyId -> companyRepository.findById(companyId)
                        .orElseThrow(() -> new NotFoundException("Company", companyId)))
                .collect(Collectors.toCollection(HashSet::new));
    }
}
