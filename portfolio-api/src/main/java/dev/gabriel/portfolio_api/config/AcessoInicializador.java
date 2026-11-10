package dev.gabriel.portfolio_api.config;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import dev.gabriel.portfolio_api.entities.Acesso;
import dev.gabriel.portfolio_api.repositories.AcessoRepository;

/**
 * "Seed" da conta do painel: roda uma vez a cada inicializacao da aplicacao (ApplicationRunner).
 *
 * Se a tabela "acessos" estiver vazia, cria a conta com ADMIN_EMAIL e o hash de ADMIN_PASSWORD.
 * Se ja existir, nao mexe: mudar o .env depois NAO troca a senha (isso exigira um fluxo proprio).
 * Assim nao ha endpoint de "cadastro" exposto na internet: a unica conta nasce da configuracao.
 */
@Component
public class AcessoInicializador implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AcessoInicializador.class);

    private final AcessoRepository acessoRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthProperties authProperties;

    public AcessoInicializador(AcessoRepository acessoRepository, PasswordEncoder passwordEncoder,
            AuthProperties authProperties) {
        this.acessoRepository = acessoRepository;
        this.passwordEncoder = passwordEncoder;
        this.authProperties = authProperties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (acessoRepository.count() > 0) {
            return;
        }
        String email = authProperties.adminEmail().trim().toLowerCase(Locale.ROOT);
        acessoRepository.save(new Acesso(email, passwordEncoder.encode(authProperties.adminSenha())));
        log.info("Conta do painel criada para {}", email);
    }
}
