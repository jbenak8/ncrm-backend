package cz.jbenak.ncrm_backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke integration test verifying that the full application context starts against
 * the self-contained in-memory H2 database (profile "test") with the "local" security chain.
 */
@SpringBootTest
@ActiveProfiles({"local", "test"})
class NcrmBackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
