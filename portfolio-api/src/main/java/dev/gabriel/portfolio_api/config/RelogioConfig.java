package dev.gabriel.portfolio_api.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * O "relogio" da aplicacao como bean. O codigo que lida com prazos (ex.: expiracao do codigo 2FA)
 * pergunta a hora para este Clock em vez de chamar Instant.now() direto.
 * Assim os testes podem simular a passagem do tempo sem esperar 10 minutos de verdade.
 */
@Configuration
public class RelogioConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
