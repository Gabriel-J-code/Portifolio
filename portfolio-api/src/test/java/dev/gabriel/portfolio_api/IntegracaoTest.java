package dev.gabriel.portfolio_api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import dev.gabriel.portfolio_api.services.EmailService;
import dev.gabriel.portfolio_api.services.JwtService;

/**
 * Base dos testes de integracao: aplicacao completa (controllers, services, seguranca, JPA)
 * contra um SQL Server real no Docker.
 *
 * - @SpringBootTest: sobe o contexto inteiro do Spring, como em producao.
 * - @AutoConfigureMockMvc: cria o MockMvc, que simula requisicoes HTTP sem abrir porta de rede.
 *   A requisicao passa pela cadeia de seguranca, validacao e GlobalExceptionHandler, como uma real.
 * - properties: credenciais e chave JWT so para os testes (nao dependem do .env: rodam em qualquer maquina/CI).
 *
 * Todas as classes de teste herdam a MESMA configuracao. Por isso o Spring reaproveita o contexto
 * (e o container) entre elas, em vez de subir tudo de novo para cada classe.
 */
@SpringBootTest(properties = {
        "ADMIN_PASSWORD=" + IntegracaoTest.ADMIN_SENHA,
        "ADMIN_EMAIL=" + IntegracaoTest.ADMIN_EMAIL,
        // Base64 de "chave-de-teste-com-32-bytes-ok!!" (32 bytes, o minimo para HS256)
        "JWT_SECRET=Y2hhdmUtZGUtdGVzdGUtY29tLTMyLWJ5dGVzLW9rISE=",
        "spring.jpa.show-sql=false",
        // Enquanto o container inicializa, o driver loga dezenas de "Prelogin error" (tentativas de conexao).
        // Sao esperados e inofensivos; so poluem a saida.
        "logging.level.com.microsoft.sqlserver.jdbc.internals.SQLServerConnection=ERROR"
})
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
public abstract class IntegracaoTest {

    protected static final String ADMIN_SENHA = "senha-de-teste";
    protected static final String ADMIN_EMAIL = "gabriel@exemplo.com";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JwtService jwtService;

    /**
     * O EmailService de verdade e substituido por um "duble" do Mockito: nenhum e-mail sai,
     * e os testes podem conferir com que dados ele foi chamado (ex.: capturar o codigo do 2FA).
     * Fica na classe base para todas as classes de teste compartilharem o MESMO contexto do Spring;
     * um @MockitoBean diferente em cada classe forcaria subir outro contexto (e outro container).
     * O Mockito "zera" o mock automaticamente depois de cada teste.
     */
    @MockitoBean
    protected EmailService emailService;

    /**
     * Adiciona "Authorization: Bearer <token>" com um token JWT REAL, emitido pelo JwtService.
     * Assim todo teste de escrita passa pelo filtro JWT de verdade (assinatura, expiracao, emissor).
     * O fluxo completo (login + codigo -> token) e testado a parte, no AuthControllerTest.
     */
    protected RequestPostProcessor comoAdmin() {
        return comToken(jwtService.gerarToken(ADMIN_EMAIL).token());
    }

    protected static RequestPostProcessor comToken(String token) {
        return request -> {
            request.addHeader("Authorization", "Bearer " + token);
            return request;
        };
    }
}
