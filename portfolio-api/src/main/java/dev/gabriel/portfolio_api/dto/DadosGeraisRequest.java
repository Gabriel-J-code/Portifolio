package dev.gabriel.portfolio_api.dto;

import org.hibernate.validator.constraints.URL;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Exemplo de JSON:
 * {
 *   "texto": "Ola, meu nome e Gabriel de Jesus, sou desenvolvedor full stack...",
 *   "disponibilidade": "Disponivel para contratacao",
 *   "fotoUrl": "https://.../foto.png"
 * }
 * Sem "disponibilidade" (ou null), o site nao exibe o selo.
 */
public record DadosGeraisRequest(

        // A coluna e NVARCHAR(MAX), mas um limite evita colar um texto gigante por engano
        @NotBlank @Size(max = 5000)
        String texto,

        @Size(max = 100)
        String disponibilidade,

        @URL @Size(max = 255)
        String fotoUrl) {
}
