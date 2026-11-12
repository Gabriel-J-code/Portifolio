package dev.gabriel.portfolio_api.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotEmpty;

/**
 * Configuracoes "app.cors.*": quais sites (origens) podem chamar a API pelo navegador.
 *
 * Fica fora do codigo porque muda por ambiente: em desenvolvimento, o Flutter roda em
 * http://localhost:<porta aleatoria>; em producao, no dominio do GitHub/Cloudflare Pages.
 */
@Validated
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(

        /*
         * Padroes de origem aceitos, ex.: "https://gabriel.dev" ou "http://localhost:[*]"
         * ([*] = qualquer porta). Origem = esquema + dominio + porta, sem caminho e sem "/" no final.
         */
        @NotEmpty
        List<String> origensPermitidas) {
}
