package com.codingshuttle.projects.airBnbApp.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * API documentation served at /api/v1/swagger-ui/index.html. Use "Authorize" with the token
 * returned by POST /auth/login to call protected endpoints from the UI.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI airbnbOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("StayEase Hotel Booking API")
                        .version("v1")
                        .description("Search hotels, book stays and manage listings. Responses are wrapped in "
                                + "{ timeStamp, data, error }. Guest endpoints need a GUEST token, /admin endpoints "
                                + "a HOTEL_MANAGER token."))
                .components(new Components().addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
    }
}
