package dev.gabriel.portfolio_api.services;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.gabriel.portfolio_api.config.AuthProperties;
import dev.gabriel.portfolio_api.dto.LoginRequest;
import dev.gabriel.portfolio_api.dto.LoginResponse;
import dev.gabriel.portfolio_api.dto.TokenResponse;
import dev.gabriel.portfolio_api.dto.VerificarCodigoRequest;
import dev.gabriel.portfolio_api.entities.Acesso;
import dev.gabriel.portfolio_api.exceptions.CodigoInvalidoException;
import dev.gabriel.portfolio_api.exceptions.CredenciaisInvalidasException;
import dev.gabriel.portfolio_api.repositories.AcessoRepository;

/**
 * Autenticacao do painel em duas etapas (2FA sempre obrigatorio).
 * Etapa 1 (aqui): e-mail + senha corretos -> gera um codigo de 6 digitos e envia por e-mail.
 * Etapa 2: codigo correto, dentro do prazo e dentro do limite de tentativas -> emite o token JWT.
 */
@Service
public class AcessoService {

    private final AcessoRepository acessoRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final AuthProperties authProperties;
    private final JwtService jwtService;
    private final Clock clock;

    // SecureRandom (e nao Random): gera numeros imprevisiveis, proprios para segredos.
    // O Random comum e previsivel: conhecendo alguns valores, da para calcular os proximos.
    private final SecureRandom random = new SecureRandom();

    // Hash "de mentira", usado quando o e-mail nao existe (ver iniciarLogin)
    private final String hashFalso;

    public AcessoService(AcessoRepository acessoRepository, PasswordEncoder passwordEncoder,
            EmailService emailService, AuthProperties authProperties, JwtService jwtService, Clock clock) {
        this.acessoRepository = acessoRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.authProperties = authProperties;
        this.jwtService = jwtService;
        this.clock = clock;
        this.hashFalso = passwordEncoder.encode("senha-falsa-so-para-gastar-o-mesmo-tempo");
    }

    /**
     * Etapa 1 do login.
     *
     * Transacional de proposito: o codigo e salvo ANTES do envio do e-mail. Se o envio falhar,
     * a excecao desfaz (rollback) o salvamento, e nao fica um codigo valido que ninguem recebeu.
     */
    @Transactional
    public LoginResponse iniciarLogin(LoginRequest dados) {
        String email = dados.email().trim().toLowerCase(Locale.ROOT);
        Optional<Acesso> encontrado = acessoRepository.findByEmailIgnoreCase(email);

        // Mesmo com e-mail inexistente, roda um BCrypt: a resposta leva o mesmo tempo nos dois casos.
        // Sem isso, um atacante mediria o tempo de resposta para descobrir se o e-mail existe
        // (e-mail errado responderia instantaneamente; e-mail certo, ~100 ms do BCrypt).
        String hashParaComparar = encontrado.map(Acesso::getSenha).orElse(hashFalso);
        boolean senhaConfere = passwordEncoder.matches(dados.senha(), hashParaComparar);
        if (encontrado.isEmpty() || !senhaConfere) {
            throw new CredenciaisInvalidasException();
        }

        Acesso acesso = encontrado.get();
        String codigo = gerarCodigo();
        Instant expiraEm = Instant.now(clock).plus(authProperties.codigoValidade());

        // Um novo login substitui o codigo anterior (so o ultimo codigo enviado vale) e zera as tentativas
        acesso.definirCodigo(passwordEncoder.encode(codigo), expiraEm);
        acessoRepository.saveAndFlush(acesso);

        emailService.enviarCodigoDeAcesso(acesso.getEmail(), codigo, authProperties.codigoValidade());

        return new LoginResponse("Codigo de acesso enviado por e-mail", mascarar(acesso.getEmail()), expiraEm);
    }

    /**
     * Etapa 2 do login: confere o codigo e, se estiver certo, emite o token JWT.
     *
     * noRollbackFor: por padrao, uma excecao DESFAZ tudo o que a transacao gravou. Aqui isso seria
     * um furo de seguranca: o "tentativas + 1" de um codigo errado seria desfeito junto com a excecao,
     * e o limite de tentativas nunca seria atingido. Com noRollbackFor, o contador e gravado mesmo
     * quando respondemos 401.
     */
    @Transactional(noRollbackFor = CodigoInvalidoException.class)
    public TokenResponse verificarCodigo(VerificarCodigoRequest dados) {
        String email = dados.email().toLowerCase(Locale.ROOT);
        Acesso acesso = acessoRepository.findByEmailIgnoreCase(email)
                .filter(a -> a.getTwoFactorCode() != null)
                // Mesma mensagem para "e-mail nao existe" e "nao ha codigo pendente": nao revela o e-mail
                .orElseThrow(() -> new CodigoInvalidoException(
                        "Nenhum codigo pendente para este e-mail; faca login novamente"));

        if (!Instant.now(clock).isBefore(acesso.getTwoFactorCodeExpiresAt())) {
            acesso.limparCodigo();
            throw new CodigoInvalidoException("Codigo expirado; faca login novamente");
        }

        if (!passwordEncoder.matches(dados.codigo(), acesso.getTwoFactorCode())) {
            int tentativas = acesso.getTwoFactorTentativas() + 1;
            int restantes = authProperties.maxTentativasCodigo() - tentativas;
            if (restantes <= 0) {
                acesso.limparCodigo(); // sem codigo pendente, nem o codigo certo funciona mais
                throw new CodigoInvalidoException("Codigo incorreto; tentativas esgotadas, faca login novamente");
            }
            acesso.setTwoFactorTentativas(tentativas);
            throw new CodigoInvalidoException("Codigo incorreto; restam " + restantes + " tentativa(s)");
        }

        // Codigo certo: ele e "queimado" (uso unico) antes de emitir o token
        acesso.limparCodigo();
        return jwtService.gerarToken(acesso.getEmail());
    }

    // --- auxiliares ---

    /** 000000 a 999999, sempre com 6 digitos (o "%06d" completa com zeros a esquerda). */
    private String gerarCodigo() {
        return "%06d".formatted(random.nextInt(1_000_000));
    }

    /** "gabriel@gmail.com" -> "g******@gmail.com" */
    static String mascarar(String email) {
        int arroba = email.indexOf('@');
        if (arroba <= 1) {
            return "*" + email.substring(Math.max(arroba, 0));
        }
        return email.charAt(0) + "*".repeat(arroba - 1) + email.substring(arroba);
    }
}
