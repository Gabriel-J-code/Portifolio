package dev.gabriel.portfolio_api.controllers;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import dev.gabriel.portfolio_api.IntegracaoTest;
import dev.gabriel.portfolio_api.repositories.TagRepository;

/**
 * CORS: simula o que o NAVEGADOR faz quando o Flutter (outro dominio) chama a API.
 *
 * - Requisicao "simples" (GET): o navegador manda o cabecalho Origin e so entrega a resposta ao site
 *   se ela voltar com Access-Control-Allow-Origin.
 * - Preflight: antes de um POST com token/JSON, o navegador pergunta com um OPTIONS
 *   ("posso fazer POST com estes cabecalhos?"). So se a resposta permitir, o POST de verdade e enviado.
 *
 * Origens permitidas nos testes: http://localhost:[*] e ORIGEM_PRODUCAO (ver IntegracaoTest).
 */
class CorsTest extends IntegracaoTest {

    private static final String ORIGEM_MALICIOSA = "https://site-malicioso.com";

    @Autowired
    private TagRepository tagRepository;

    @AfterEach
    void limpar() {
        tagRepository.deleteAll();
    }

    /** O OPTIONS que o navegador envia antes de um POST autenticado com JSON. */
    private ResultActions preflightDeCriarTag(String origem) throws Exception {
        return mockMvc.perform(options("/api/tags")
                .header(HttpHeaders.ORIGIN, origem)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization,content-type"));
    }

    @ParameterizedTest(name = "origem {0}")
    @ValueSource(strings = { "http://localhost:5000", "http://localhost:61234", ORIGEM_PRODUCAO })
    @DisplayName("Preflight de origem permitida -> 200, sem exigir token, liberando metodo e cabecalhos")
    void preflightPermitido(String origem) throws Exception {
        preflightDeCriarTag(origem)
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origem))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("POST")))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, containsString("authorization")))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600"));
    }

    @ParameterizedTest(name = "origem {0}")
    @ValueSource(strings = { ORIGEM_MALICIOSA, "http://portfolio.exemplo.dev", "https://portfolio.exemplo.dev.evil.com" })
    @DisplayName("Preflight de origem nao permitida (inclusive parecidas com a verdadeira) -> 403")
    void preflightNegado(String origem) throws Exception {
        preflightDeCriarTag(origem)
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    @DisplayName("GET publico de origem permitida -> 200 com Access-Control-Allow-Origin")
    void getDeOrigemPermitida() throws Exception {
        mockMvc.perform(get("/api/tags").header(HttpHeaders.ORIGIN, ORIGEM_PRODUCAO))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGEM_PRODUCAO));
    }

    @Test
    @DisplayName("GET de origem nao permitida -> 403 (o navegador nao entregaria a resposta ao site)")
    void getDeOrigemNegada() throws Exception {
        mockMvc.perform(get("/api/tags").header(HttpHeaders.ORIGIN, ORIGEM_MALICIOSA))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Sem cabecalho Origin (curl, Postman, servidor) -> CORS nao se aplica, segue normal")
    void semOrigin() throws Exception {
        mockMvc.perform(get("/api/tags"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    @DisplayName("POST com token de origem permitida -> 201 e o Location fica visivel para o front")
    void postExpoeLocation() throws Exception {
        mockMvc.perform(post("/api/tags").with(comoAdmin())
                        .header(HttpHeaders.ORIGIN, ORIGEM_PRODUCAO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"nome\": \"Cors\", \"cor\": \"#000000\" }"))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGEM_PRODUCAO))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, containsString("Location")));
    }

    @Test
    @DisplayName("CORS nao substitui o login: origem permitida sem token continua 401")
    void origemPermitidaSemToken() throws Exception {
        mockMvc.perform(post("/api/tags")
                        .header(HttpHeaders.ORIGIN, ORIGEM_PRODUCAO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"nome\": \"Cors\", \"cor\": \"#000000\" }"))
                .andExpect(status().isUnauthorized());
    }
}
