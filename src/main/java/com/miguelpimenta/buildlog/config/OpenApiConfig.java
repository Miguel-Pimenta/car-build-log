package com.miguelpimenta.buildlog.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  // Arbitrary internal key: links the scheme definition below to the requirement.
  private static final String BEARER_SCHEME = "bearer-jwt";

  @Bean
  public OpenAPI carBuildLogOpenAPI() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Car Build Log API")
                .description("Track project-car builds: vehicles, modifications, and dyno results.")
                .version("v1"))
        // Declare a reusable security scheme describing your Authorization: Bearer
        // <JWT> header.
        .components(
            new Components()
                .addSecuritySchemes(
                    BEARER_SCHEME,
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
        // Apply that scheme to every operation, so the Authorize button + lock icons
        // show up.
        .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
  }
}
