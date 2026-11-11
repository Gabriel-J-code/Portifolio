package dev.gabriel.portfolio_api.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mail.MailSendException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

import dev.gabriel.portfolio_api.IntegracaoTest;
import dev.gabriel.portfolio_api.entities.Acesso;
import dev.gabriel.portfolio_api.repositories.AcessoRepository;
import dev.gabriel.portfolio_api.repositories.TagRepository;

class AuthControllerTest extends IntegracaoTest {

    private static final String LOGIN = "/api/auth/login";
    private static final String VERIFICAR = "/api/auth/verify-2fa";

    @Autowired
    private AcessoRepository acessoRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /** A conta e criada pelo AcessoInicializador na subida; aqui so limpamos o codigo entre testes. */
    @BeforeEach
    void limparCodigo() {
        Acesso acesso = conta();
        acesso.limparCodigo();
        acessoRepository.save(acesso);
    }

    private Acesso conta() {
        return acessoRepository.findByEmailIgnoreCase(ADMIN_EMAIL).orElseThrow();
    }

    private String json(String email, String senha) {
        return """
                { "email": "%s", "senha": "%s" }
                """.formatted(email, senha);
    }

    /** Captura o codigo que o AcessoService mandou o EmailService enviar (o ultimo, se houver varios). */
    private String codigoEnviado() {
        ArgumentCaptor<String> codigo = ArgumentCaptor.forClass(String.class);
        verify(emailService, atLeastOnce()).enviarCodigoDeAcesso(eq(ADMIN_EMAIL), codigo.capture(), any(Duration.class));
        return codigo.getValue();
    }

    /** Etapa 1 completa: faz login e devolve o codigo que "chegou por e-mail". */
    private String loginECodigo() throws Exception {
        mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON).content(json(ADMIN_EMAIL, ADMIN_SENHA)))
                .andExpect(status().isOk());
        return codigoEnviado();
    }

    /** Etapa 2: envia o codigo. */
    private ResultActions verificar(String codigo) throws Exception {
        return mockMvc.perform(post(VERIFICAR).contentType(MediaType.APPLICATION_JSON).content("""
                { "email": "%s", "codigo": "%s" }
                """.formatted(ADMIN_EMAIL, codigo)));
    }

    /** Um codigo de 6 digitos garantidamente diferente do certo. */
    private static String errado(String certo) {
        return certo.equals("000000") ? "111111" : "000000";
    }

    // --- conta inicial (seed) ---

    @Test
    @DisplayName("Na subida, a conta do painel e criada a partir da configuracao, com a senha em hash")
    void contaCriadaNaSubida() {
        assertThat(acessoRepository.count()).isEqualTo(1);
        Acesso acesso = conta();
        assertThat(acesso.getSenha()).isNotEqualTo(ADMIN_SENHA);
        assertThat(passwordEncoder.matches(ADMIN_SENHA, acesso.getSenha())).isTrue();
    }

    // --- login correto ---

    @Test
    @DisplayName("Login correto -> 200, sem token, e-mail mascarado e codigo enviado")
    void loginCorreto() throws Exception {
        mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON).content(json(ADMIN_EMAIL, ADMIN_SENHA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensagem").value("Codigo de acesso enviado por e-mail"))
                .andExpect(jsonPath("$.emailMascarado").value("g******@exemplo.com"))
                .andExpect(jsonPath("$.expiraEm").exists())
                .andExpect(jsonPath("$.token").doesNotExist());

        assertThat(codigoEnviado()).matches("\\d{6}");
    }

    @Test
    @DisplayName("O banco guarda o HASH do codigo (nunca o codigo) e a validade de ~10 minutos")
    void codigoGuardadoComoHash() throws Exception {
        Instant antes = Instant.now();
        mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON).content(json(ADMIN_EMAIL, ADMIN_SENHA)))
                .andExpect(status().isOk());

        String codigo = codigoEnviado();
        Acesso acesso = conta();
        assertThat(acesso.getTwoFactorCode()).isNotEqualTo(codigo);
        assertThat(passwordEncoder.matches(codigo, acesso.getTwoFactorCode())).isTrue();
        assertThat(acesso.getTwoFactorCodeExpiresAt())
                .isBetween(antes.plus(Duration.ofMinutes(9)), antes.plus(Duration.ofMinutes(11)));
    }

    @Test
    @DisplayName("E-mail com maiusculas e espacos tambem funciona")
    void emailNormalizado() throws Exception {
        mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON)
                        .content(json("  GABRIEL@Exemplo.com ", ADMIN_SENHA)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Um novo login substitui o codigo anterior: so o ultimo vale")
    void novoLoginSubstituiCodigo() throws Exception {
        mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON).content(json(ADMIN_EMAIL, ADMIN_SENHA)));
        String hashPrimeiro = conta().getTwoFactorCode();

        mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON).content(json(ADMIN_EMAIL, ADMIN_SENHA)));
        String hashSegundo = conta().getTwoFactorCode();

        assertThat(hashSegundo).isNotEqualTo(hashPrimeiro);
        verify(emailService, times(2)).enviarCodigoDeAcesso(anyString(), anyString(), any());
    }

    // --- login incorreto ---

    @Test
    @DisplayName("Senha errada -> 401 e nenhum e-mail enviado")
    void senhaErrada() throws Exception {
        mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON).content(json(ADMIN_EMAIL, "errada")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("E-mail ou senha invalidos"));

        verify(emailService, never()).enviarCodigoDeAcesso(anyString(), anyString(), any());
        assertThat(conta().getTwoFactorCode()).isNull();
    }

    @Test
    @DisplayName("E-mail inexistente -> 401 com a MESMA mensagem da senha errada")
    void emailInexistente() throws Exception {
        mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON)
                        .content(json("outro@exemplo.com", ADMIN_SENHA)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("E-mail ou senha invalidos"));

        verify(emailService, never()).enviarCodigoDeAcesso(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("E-mail mal formatado ou campos vazios -> 400")
    void validacao() throws Exception {
        mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON).content(json("nao-e-email", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.email").exists())
                .andExpect(jsonPath("$.erros.senha").exists());
    }

    // --- falha no envio ---

    @Test
    @DisplayName("Se o e-mail nao puder ser enviado -> 503 e o codigo NAO fica salvo (rollback)")
    void falhaNoEnvio() throws Exception {
        doThrow(new MailSendException("SMTP fora do ar"))
                .when(emailService).enviarCodigoDeAcesso(anyString(), anyString(), any());

        mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON).content(json(ADMIN_EMAIL, ADMIN_SENHA)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.detail").value(
                        "Nao foi possivel enviar o e-mail agora; tente novamente em instantes"));

        assertThat(conta().getTwoFactorCode()).isNull();
    }

    // ===================== Etapa 2: verify-2fa =====================

    @Test
    @DisplayName("Fluxo completo: login -> codigo -> token, e o token libera a escrita")
    void fluxoCompleto() throws Exception {
        String codigo = loginECodigo();

        String resposta = verificar(codigo)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.expiraEm").exists())
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(resposta, "$.token");

        // O token obtido pelo fluxo real funciona numa rota protegida
        mockMvc.perform(post("/api/tags").with(comToken(token)).contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"nome\": \"Token-Real\", \"cor\": \"#000000\" }"))
                .andExpect(status().isCreated());
        tagRepository.deleteAll();
    }

    @Test
    @DisplayName("O codigo so vale uma vez: reutiliza-lo -> 401")
    void codigoDeUsoUnico() throws Exception {
        String codigo = loginECodigo();
        verificar(codigo).andExpect(status().isOk());

        verificar(codigo)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Nenhum codigo pendente para este e-mail; faca login novamente"));
        assertThat(conta().getTwoFactorCode()).isNull();
    }

    @Test
    @DisplayName("Codigo errado -> 401 dizendo quantas tentativas restam, e o erro FICA gravado")
    void codigoErrado() throws Exception {
        String codigo = loginECodigo();

        verificar(errado(codigo))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Codigo incorreto; restam 4 tentativa(s)"));

        // Prova do noRollbackFor: mesmo com a excecao, o contador foi salvo no banco
        assertThat(conta().getTwoFactorTentativas()).isEqualTo(1);
    }

    @Test
    @DisplayName("5 erros esgotam o codigo: depois disso nem o codigo certo funciona")
    void tentativasEsgotadas() throws Exception {
        String codigo = loginECodigo();
        for (int i = 1; i <= 4; i++) {
            verificar(errado(codigo)).andExpect(status().isUnauthorized());
        }
        verificar(errado(codigo))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Codigo incorreto; tentativas esgotadas, faca login novamente"));

        verificar(codigo).andExpect(status().isUnauthorized());
        assertThat(conta().getTwoFactorCode()).isNull();
    }

    @Test
    @DisplayName("Um novo login zera as tentativas")
    void novoLoginZeraTentativas() throws Exception {
        String primeiro = loginECodigo();
        verificar(errado(primeiro)).andExpect(status().isUnauthorized());
        assertThat(conta().getTwoFactorTentativas()).isEqualTo(1);

        String segundo = loginECodigo();
        assertThat(conta().getTwoFactorTentativas()).isZero();
        verificar(segundo).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Codigo expirado -> 401, mesmo sendo o codigo certo")
    void codigoExpirado() throws Exception {
        String codigo = loginECodigo();
        // Simula a passagem do tempo: o prazo do codigo vira "1 segundo atras"
        Acesso acesso = conta();
        acesso.setTwoFactorCodeExpiresAt(Instant.now().minusSeconds(1));
        acessoRepository.save(acesso);

        verificar(codigo)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Codigo expirado; faca login novamente"));
        assertThat(conta().getTwoFactorCode()).isNull();
    }

    @Test
    @DisplayName("Sem login antes, ou com e-mail inexistente -> 401 com a mesma mensagem")
    void semCodigoPendente() throws Exception {
        verificar("123456")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Nenhum codigo pendente para este e-mail; faca login novamente"));

        mockMvc.perform(post(VERIFICAR).contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"email\": \"outro@exemplo.com\", \"codigo\": \"123456\" }"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Nenhum codigo pendente para este e-mail; faca login novamente"));
    }

    @Test
    @DisplayName("Codigo fora do formato de 6 digitos -> 400 (nao conta como tentativa)")
    void formatoInvalido() throws Exception {
        loginECodigo();

        verificar("12ab")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.codigo").value("deve ter exatamente 6 digitos"));
        assertThat(conta().getTwoFactorTentativas()).isZero();
    }
}
