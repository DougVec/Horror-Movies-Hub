package com.douglas.horrorhub.shared;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/** Swagger UI em /swagger-ui.html; use POST /auth/login e cole o token em "Authorize". */
@Configuration
@OpenAPIDefinition(
        info = @Info(title = "Horror Movie Hub", version = "0.1.0",
                description = "Lista de filmes de terror, reviews com medômetro, alertas de estreia/streaming e gerador de maratona."),
        security = @SecurityRequirement(name = "bearer"))
@SecurityScheme(name = "bearer", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {
}
