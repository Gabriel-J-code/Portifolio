package dev.gabriel.portfolio_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Exemplo de JSON: { "nome": "Spring Boot", "cor": "#6DB33F" }
 *
 * Tags sao as "etiquetas" de tecnologia exibidas nos cards de projeto.
 * O nome e unico, sem diferenciar maiusculas/minusculas ("Java" e "java" sao a mesma tag).
 */
public record TagRequest(

        // Curto de proposito: tag longa quebra o layout do card no site
        @NotBlank @Size(max = 50)
        String nome,

        @NotBlank
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "deve estar no formato #RRGGBB")
        String cor) {
}
