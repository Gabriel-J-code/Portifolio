package dev.gabriel.portfolio_api.config;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

/**
 * Metadados do Swagger (http://localhost:8080/swagger-ui.html).
 * O SecurityScheme "admin" faz aparecer o botao "Authorize", onde se cola o token JWT
 * obtido em /api/auth/verify-2fa (sem o prefixo "Bearer": o Swagger adiciona sozinho).
 */
@Configuration
@OpenAPIDefinition(info = @Info(
        title = "Portfolio API",
        version = "v1",
        description = "API do portfolio de Gabriel de Jesus: projetos, competencias, contatos e dados gerais."))
@SecurityScheme(name = "admin", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {
}
