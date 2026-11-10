package dev.gabriel.portfolio_api.dto;

import org.hibernate.validator.constraints.URL;

import dev.gabriel.portfolio_api.entities.TipoContato;
import dev.gabriel.portfolio_api.validacao.ValorCompativelComTipo;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Exemplos de JSON:
 * { "tipo": "LINKEDIN", "valor": "https://www.linkedin.com/in/...", "ordem": 1 }
 * { "tipo": "EMAIL",    "valor": "nome@gmail.com",                  "ordem": 2 }
 *
 * Para EMAIL, o valor e o endereco puro; o front monta o link "mailto:".
 */
@ValorCompativelComTipo // regra entre campos: o formato do "valor" depende do "tipo"
public record ContatoRequest(

        @NotNull
        TipoContato tipo,

        @NotBlank @Size(max = 255)
        String valor,

        @URL @Size(max = 255)
        String iconUrl,

        @NotNull @Min(1)
        Integer ordem) {
}
