package com.eventreg.userservice.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import java.util.Map;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info =
        @Info(
            title = "user-service API",
            version = "1.0",
            description = "Auth facade over Keycloak"))
public class OpenApiConfig {

  @Bean
  OpenApiCustomizer disableReadMeExplorer() {
    return openApi ->
        openApi.getPaths().values().stream()
            .flatMap(pathItem -> pathItem.readOperations().stream())
            .forEach(
                operation -> operation.addExtension("x-readme", Map.of("explorer-enabled", false)));
  }
}
