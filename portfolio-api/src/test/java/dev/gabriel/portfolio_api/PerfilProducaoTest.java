package dev.gabriel.portfolio_api;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import dev.gabriel.portfolio_api.repositories.TagRepository;
import dev.gabriel.portfolio_api.services.JwtService;

/**
 * Sobe a aplicacao com o perfil "prod" (application-prod.properties) e confere o que muda em producao.
 *
 * Nao herda de IntegracaoTest de proposito: o perfil e diferente, entao o Spring precisa de outro contexto.
 * As variaveis obrigatorias de producao recebem valores ficticios; o banco continua vindo do
 * Testcontainers (@ServiceConnection tem prioridade sobre DB_URL).
 */
@SpringBootTest(properties = {
        "DB_URL=jdbc:postgresql://ignorado-pelo-testcontainers/portfolio",
        "DB_USER=ignorado",
        "DB_PASSWORD=ignorado",
        "MAIL_HOST=smtp.exemplo.com",
        "MAIL_USERNAME=usuario",
        "MAIL_PASSWORD=senha",
        "MAIL_FROM=portfolio@exemplo.com",
        "CORS_ORIGINS=" + PerfilProducaoTest.ORIGEM_DO_FRONT,
        "ADMIN_EMAIL=gabriel@exemplo.com",
        "ADMIN_PASSWORD=senha-de-teste",
        "JWT_SECRET=Y2hhdmUtZGUtdGVzdGUtY29tLTMyLWJ5dGVzLW9rISE="
})
@ActiveProfiles("prod")
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
class PerfilProducaoTest {

    static final String ORIGEM_DO_FRONT = "https://gabriel.exemplo.dev";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private TagRepository tagRepository;

    @AfterEach
    void limpar() {
        tagRepository.deleteAll();
    }

    @Test
    @DisplayName("Swagger UI e especificacao OpenAPI desligados -> 404")
    void swaggerDesligado() throws Exception {
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isNotFound());
        mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Leitura publica continua funcionando")
    void leituraPublica() throws Exception {
        mockMvc.perform(get("/api/tags")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("CORS: localhost (padrao de desenvolvimento) nao e aceito em producao -> 403")
    void localhostNegado() throws Exception {
        mockMvc.perform(options("/api/tags")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5000")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Atras do proxy HTTPS (X-Forwarded-*), o Location do 201 sai com https e o dominio publico")
    void locationComHttpsAtrasDoProxy() throws Exception {
        String token = jwtService.gerarToken("gabriel@exemplo.com").token();

        // O proxy do Cloud Run recebe em HTTPS e repassa em HTTP, avisando nos cabecalhos X-Forwarded-*
        mockMvc.perform(post("/api/tags")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .header("X-Forwarded-Proto", "https")
                        .header("X-Forwarded-Host", "api.exemplo.dev")
                        .header("X-Forwarded-Port", "443")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"nome\": \"Prod\", \"cor\": \"#000000\" }"))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, startsWith("https://api.exemplo.dev/api/tags/")));
    }
}
