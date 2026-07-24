package cz.jbenak.ncrm_backend.services;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import cz.jbenak.ncrm_backend.model.dto.AddressDto;
import cz.jbenak.ncrm_backend.model.dto.ares.AresSubjectDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-11
 * Service looking up economic subjects in the Czech ARES registry (https://ares.gov.cz).
 * Used to pre-fill customer and company data when creating or editing them in the frontend.
 * The same DTO can be filled from similar EU registries (e.g. VIES) in the future.
 */
@Slf4j
@Service
public class AresService {

    private final RestClient restClient;

    public AresService(RestClient.Builder builder,
                       @Value("${ncrm.ares.base-url:https://ares.gov.cz/ekonomicke-subjekty-v-be/rest}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    /**
     * Looks up an economic subject by its registration id (IČO).
     */
    public AresSubjectDto findByRegistrationId(String registrationId) {
        if (registrationId == null || !registrationId.matches("\\d{8}")) {
            throw new IllegalArgumentException("Registration id (IČO) must be an 8-digit number");
        }
        log.debug("Looking up economic subject {} in ARES", registrationId);
        AresEconomicSubject subject = restClient.get()
                .uri("/ekonomicke-subjekty/{ico}", registrationId)
                .retrieve()
                .body(AresEconomicSubject.class);
        if (subject == null || subject.ico() == null) {
            log.info("Economic subject {} not found in ARES", registrationId);
            throw new NotFoundException("Economic subject with registration id " + registrationId + " not found in ARES");
        }
        log.info("ARES lookup of {} succeeded: {}", registrationId, subject.obchodniJmeno());
        return new AresSubjectDto(subject.ico(), subject.dic(), subject.obchodniJmeno(), subject.pravniForma(),
                mapAddress(subject.sidlo()));
    }

    private static AddressDto mapAddress(AresAddress sidlo) {
        if (sidlo == null) {
            return null;
        }
        String houseNumber = sidlo.cisloDomovni() == null ? null : String.valueOf(sidlo.cisloDomovni());
        String zipCode = sidlo.psc() == null ? null : String.valueOf(sidlo.psc());
        String streetNumber = sidlo.cisloOrientacni() == null ? null : String.valueOf(sidlo.cisloOrientacni());
        return new AddressDto(null, sidlo.nazevUlice(), houseNumber, streetNumber, sidlo.nazevObce(), zipCode,
                sidlo.kodStatu(), sidlo.nazevStatu());
    }

    /**
     * Minimal projection of the ARES REST response.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record AresEconomicSubject(String ico, String dic, String obchodniJmeno, String pravniForma, AresAddress sidlo) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AresAddress(String nazevUlice, Integer cisloDomovni, Integer cisloOrientacni, String nazevObce, Integer psc,
                       String kodStatu, String nazevStatu) {
    }
}
