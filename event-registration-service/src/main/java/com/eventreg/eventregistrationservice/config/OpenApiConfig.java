package com.eventreg.eventregistrationservice.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import java.util.Map;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Configures OpenAPI documentation for the service. */
@Configuration
@OpenAPIDefinition(
    info = @Info(title = "My API", version = "1.0", description = "event-registration-serviceAPi"),
    security = @SecurityRequirement(name = "bearerAuth"))
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT")
public class OpenApiConfig {

  /** Disables the readme explorer extension on all documented operations. */
  @Bean
  OpenApiCustomizer disableReadMeExplorer() {
    return openApi ->
        openApi.getPaths().values().stream()
            .flatMap(pathItem -> pathItem.readOperations().stream())
            .forEach(
                operation -> operation.addExtension("x-readme", Map.of("explorer-enabled", false)));
  }
}
