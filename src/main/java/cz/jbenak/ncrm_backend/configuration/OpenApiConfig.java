package cz.jbenak.ncrm_backend.configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-11
 * Swagger / OpenAPI documentation of the nCRM REST API (springdoc-openapi).
 * The interactive documentation is available at /swagger-ui.html and the
 * machine-readable specification at /v3/api-docs. The API is secured either
 * by a Keycloak-issued JWT bearer token (prod) or HTTP Basic (local testing).
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";
    private static final String BASIC_SCHEME = "basicAuth";

    @Bean
    public OpenAPI ncrmOpenApi(@Value("${info.title:nCRM Backend}") String title) {
        return new OpenAPI()
                .info(new Info()
                        .title(title)
                        .description("REST API of the nCRM application: customers, meetings, orders, "
                                + "marketing campaigns with AI content generation, dashboards, "
                                + "JasperReports PDF reports and ARES lookups.")
                        .version("v1")
                        .contact(new Contact().name("Jan Benák"))
                        .license(new License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0")))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Keycloak issued JWT access token (prod profile)."))
                        .addSecuritySchemes(BASIC_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("basic")
                                .description("HTTP Basic with in-memory test users (local profile).")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .addSecurityItem(new SecurityRequirement().addList(BASIC_SCHEME));
    }
}
