package dev.gabriel.portfolio_api.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

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
        // @Pattern em cada item: barra valores que nao sao origens, como o texto literal "${CORS_ORIGINS}"
        // que sobra quando a variavel nao foi definida em producao. Assim a API falha ao subir.
        @NotEmpty
        List<@Pattern(regexp = "^https?://[^/\\s]+$", message = "deve ser uma origem como https://dominio.com (sem caminho e sem / no final)") String> origensPermitidas) {
}
