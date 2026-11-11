package dev.gabriel.portfolio_api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Exemplo: { "email": "gabriel@exemplo.com", "codigo": "042137" }
 *
 * O e-mail vem de novo porque a API nao guarda sessao (stateless): cada requisicao
 * precisa dizer de quem e. O painel ja o tem da etapa 1.
 */
public record VerificarCodigoRequest(

        @NotBlank @Email @Size(max = 255)
        String email,

        @NotBlank
        @Pattern(regexp = "^\\d{6}$", message = "deve ter exatamente 6 digitos")
        String codigo) {

    public VerificarCodigoRequest {
        if (email != null) {
            email = email.trim();
        }
        if (codigo != null) {
            codigo = codigo.trim();
        }
    }
}
