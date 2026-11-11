package dev.gabriel.portfolio_api.controllers;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

import dev.gabriel.portfolio_api.IntegracaoTest;
import dev.gabriel.portfolio_api.config.JwtConfig;
import dev.gabriel.portfolio_api.repositories.TagRepository;

/**
 * Tenta "enganar" o filtro JWT de varias formas. Todas precisam dar 401.
 * Usa uma rota protegida qualquer (POST /api/tags) como alvo.
 */
class SegurancaJwtTest extends IntegracaoTest {

    private static final String ROTA_PROTEGIDA = "/api/tags";

    @Autowired
    private JwtEncoder jwtEncoder; // o encoder REAL da aplicacao (chave certa)

    @Autowired
    private TagRepository tagRepository;

    @AfterEach
    void limpar() {
        tagRepository.deleteAll();
    }

    private ResultActions criarTag(RequestPostProcessor autenticacao) throws Exception {
        return mockMvc.perform(post(ROTA_PROTEGIDA).with(autenticacao).contentType(MediaType.APPLICATION_JSON)
                .content("{ \"nome\": \"Seguranca\", \"cor\": \"#000000\" }"));
    }

    /** Monta um token com o encoder informado e claims escolhidas pelo teste. */
    private static String token(JwtEncoder encoder, String emissor, Instant emitidoEm, Instant expiraEm) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(emissor).subject(ADMIN_EMAIL).issuedAt(emitidoEm).expiresAt(expiraEm).build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    @Test
    @DisplayName("Token valido -> 201 (controle: prova que a rota funciona com token)")
    void tokenValido() throws Exception {
        criarTag(comoAdmin()).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Sem token -> 401")
    void semToken() throws Exception {
        mockMvc.perform(post(ROTA_PROTEGIDA).contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"nome\": \"X\", \"cor\": \"#000000\" }"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Token expirado (assinatura certa) -> 401")
    void tokenExpirado() throws Exception {
        Instant agora = Instant.now();
        String expirado = token(jwtEncoder, JwtConfig.EMISSOR, agora.minusSeconds(7200), agora.minusSeconds(3600));

        criarTag(comToken(expirado)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Token assinado com OUTRA chave -> 401 (alguem tentando forjar um token)")
    void outraChave() throws Exception {
        byte[] chaveFalsa = "uma-chave-qualquer-de-32-bytes!!".getBytes(StandardCharsets.UTF_8);
        JwtEncoder falsificador = new NimbusJwtEncoder(new ImmutableSecret<>(new SecretKeySpec(chaveFalsa, "HmacSHA256")));
        String forjado = token(falsificador, JwtConfig.EMISSOR, Instant.now(), Instant.now().plusSeconds(3600));

        criarTag(comToken(forjado)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Token adulterado (conteudo trocado, assinatura original) -> 401")
    void tokenAdulterado() throws Exception {
        String original = jwtService.gerarToken(ADMIN_EMAIL).token();
        String[] partes = original.split("\\."); // cabecalho.conteudo.assinatura

        // Troca o "sub" do conteudo e mantem a assinatura antiga
        Base64.Decoder dec = Base64.getUrlDecoder();
        Base64.Encoder enc = Base64.getUrlEncoder().withoutPadding();
        String conteudo = new String(dec.decode(partes[1]), StandardCharsets.UTF_8)
                .replace(ADMIN_EMAIL, "invasor@exemplo.com");
        String adulterado = partes[0] + "." + enc.encodeToString(conteudo.getBytes(StandardCharsets.UTF_8)) + "." + partes[2];

        criarTag(comToken(adulterado)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Token com outro emissor (iss) -> 401")
    void outroEmissor() throws Exception {
        String deOutroSistema = token(jwtEncoder, "outro-sistema", Instant.now(), Instant.now().plusSeconds(3600));

        criarTag(comToken(deOutroSistema)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("O HTTP Basic temporario foi removido: usuario e senha nao abrem mais a escrita -> 401")
    void basicNaoFuncionaMais() throws Exception {
        criarTag(httpBasic("admin", ADMIN_SENHA)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Leitura publica continua sem token")
    void leituraPublica() throws Exception {
        mockMvc.perform(get(ROTA_PROTEGIDA)).andExpect(status().isOk());
    }
}
