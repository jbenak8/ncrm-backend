package cz.jbenak.ncrm_backend.services;

import cz.jbenak.ncrm_backend.model.dto.ares.AresSubjectDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-11
 * Unit tests of {@link AresService} using a mocked ARES REST server.
 */
class AresServiceTest {

    private static final String BASE_URL = "https://ares.test/rest";

    private AresService aresService;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        aresService = new AresService(builder, BASE_URL);
    }

    @Test
    void findByRegistrationIdMapsAresResponse() {
        server.expect(requestTo(BASE_URL + "/ekonomicke-subjekty/12345678"))
                .andRespond(withSuccess("""
                        {
                          "ico": "12345678",
                          "dic": "CZ12345678",
                          "obchodniJmeno": "Test s.r.o.",
                          "pravniForma": "112",
                          "sidlo": {
                            "nazevUlice": "Hlavní",
                            "cisloDomovni": 12,
                            "nazevObce": "Praha",
                            "psc": 11000,
                            "kodStatu": "CZ",
                            "nazevStatu": "Česká republika"
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        AresSubjectDto subject = aresService.findByRegistrationId("12345678");

        assertThat(subject.registrationId()).isEqualTo("12345678");
        assertThat(subject.vatId()).isEqualTo("CZ12345678");
        assertThat(subject.name()).isEqualTo("Test s.r.o.");
        assertThat(subject.address().street()).isEqualTo("Hlavní");
        assertThat(subject.address().city()).isEqualTo("Praha");
        assertThat(subject.address().countryIsoCode()).isEqualTo("CZ");
        server.verify();
    }

    @Test
    void findByRegistrationIdRejectsInvalidIco() {
        assertThatThrownBy(() -> aresService.findByRegistrationId("abc"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
