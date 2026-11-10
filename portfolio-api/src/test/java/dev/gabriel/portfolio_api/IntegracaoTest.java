package dev.gabriel.portfolio_api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;

/**
 * Base dos testes de integracao: aplicacao completa (controllers, services, seguranca, JPA)
 * contra um SQL Server real no Docker.
 *
 * - @SpringBootTest: sobe o contexto inteiro do Spring, como em producao.
 * - @AutoConfigureMockMvc: cria o MockMvc, que simula requisicoes HTTP sem abrir porta de rede.
 *   A requisicao passa pela cadeia de seguranca, validacao e GlobalExceptionHandler, como uma real.
 * - properties: senha do admin so para os testes (nao depende do .env, entao roda em qualquer maquina/CI).
 *
 * Todas as classes de teste herdam a MESMA configuracao. Por isso o Spring reaproveita o contexto
 * (e o container) entre elas, em vez de subir tudo de novo para cada classe.
 */
@SpringBootTest(properties = {
        "ADMIN_USER=" + IntegracaoTest.ADMIN_USER,
        "ADMIN_PASSWORD=" + IntegracaoTest.ADMIN_SENHA,
        "spring.jpa.show-sql=false",
        // Enquanto o container inicializa, o driver loga dezenas de "Prelogin error" (tentativas de conexao).
        // Sao esperados e inofensivos; so poluem a saida.
        "logging.level.com.microsoft.sqlserver.jdbc.internals.SQLServerConnection=ERROR"
})
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
public abstract class IntegracaoTest {

    protected static final String ADMIN_USER = "admin";
    protected static final String ADMIN_SENHA = "senha-de-teste";

    @Autowired
    protected MockMvc mockMvc;

    /** Adiciona o cabecalho "Authorization: Basic ..." com as credenciais de teste. */
    protected static RequestPostProcessor comoAdmin() {
        return httpBasic(ADMIN_USER, ADMIN_SENHA);
    }
}
