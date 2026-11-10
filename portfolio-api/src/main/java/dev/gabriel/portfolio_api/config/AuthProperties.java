package dev.gabriel.portfolio_api.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Configuracoes "app.auth.*" do application.properties, num objeto tipado.
 *
 * Por que nao varios @Value? Aqui fica tudo num lugar so, com tipos certos (Duration em vez de texto)
 * e validado na inicializacao: se ADMIN_EMAIL estiver faltando, a aplicacao nem sobe,
 * em vez de falhar so quando alguem tentar logar.
 */
@Validated
@ConfigurationProperties(prefix = "app.auth")
public record AuthProperties(

        @NotBlank @Email
        String adminEmail,

        @NotBlank
        String adminSenha,

        @NotNull
        Duration codigoValidade,

        @NotBlank @Email
        String remetente) {
}
