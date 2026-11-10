package dev.gabriel.portfolio_api.dto;

import org.hibernate.validator.constraints.URL;

import dev.gabriel.portfolio_api.entities.CategoriaCompetencia;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Exemplo de JSON:
 * { "nome": "Java", "categoria": "LINGUAGEM", "iconUrl": "https://.../java.svg", "cor": "#E76F00" }
 */
public record CompetenciaRequest(

        @NotBlank @Size(max = 255)
        String nome,

        @NotNull
        CategoriaCompetencia categoria,

        // Opcional: soft skills normalmente nao tem icone
        @URL @Size(max = 255)
        String iconUrl,

        // Opcional; quando vier, tem que ser hexadecimal "#RRGGBB" (formato que o Flutter converte facil)
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "deve estar no formato #RRGGBB")
        String cor) {
}
