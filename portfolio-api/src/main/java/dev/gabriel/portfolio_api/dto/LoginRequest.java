package dev.gabriel.portfolio_api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Exemplo: { "email": "gabriel@exemplo.com", "senha": "..." } */
public record LoginRequest(

        @NotBlank @Email @Size(max = 255)
        String email,

        // Limite: o BCrypt so considera os primeiros 72 bytes; acima disso o resto seria ignorado
        @NotBlank @Size(max = 72)
        String senha) {

    /**
     * Construtor compacto do record: roda quando o Jackson cria o objeto, ANTES da validacao.
     * Tira os espacos das pontas do e-mail (comum ao colar), para o @Email nao recusar
     * "  gabriel@exemplo.com ". A senha NAO e alterada: espacos podem fazer parte dela.
     */
    public LoginRequest {
        if (email != null) {
            email = email.trim();
        }
    }
}
