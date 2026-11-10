package dev.gabriel.portfolio_api.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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

import dev.gabriel.portfolio_api.IntegracaoTest;
import dev.gabriel.portfolio_api.entities.Acesso;
import dev.gabriel.portfolio_api.repositories.AcessoRepository;

class AuthControllerTest extends IntegracaoTest {

    private static final String LOGIN = "/api/auth/login";

    @Autowired
    private AcessoRepository acessoRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /** A conta e criada pelo AcessoInicializador na subida; aqui so limpamos o codigo entre testes. */
    @BeforeEach
    void limparCodigo() {
        Acesso acesso = conta();
        acesso.setTwoFactorCode(null);
        acesso.setTwoFactorCodeExpiresAt(null);
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

    /** Captura o codigo que o AcessoService mandou o EmailService enviar. */
    private String codigoEnviado() {
        ArgumentCaptor<String> codigo = ArgumentCaptor.forClass(String.class);
        verify(emailService).enviarCodigoDeAcesso(eq(ADMIN_EMAIL), codigo.capture(), any(Duration.class));
        return codigo.getValue();
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
}
