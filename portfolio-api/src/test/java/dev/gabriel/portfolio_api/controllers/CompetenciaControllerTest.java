package dev.gabriel.portfolio_api.controllers;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import com.jayway.jsonpath.JsonPath;

import dev.gabriel.portfolio_api.IntegracaoTest;
import dev.gabriel.portfolio_api.repositories.CompetenciaRepository;

class CompetenciaControllerTest extends IntegracaoTest {

    private static final String URL = "/api/competencias";

    @Autowired
    private CompetenciaRepository competenciaRepository;

    @BeforeEach
    void limparBanco() {
        competenciaRepository.deleteAll();
    }

    private int criar(String nome, String categoria) throws Exception {
        String resposta = mockMvc.perform(post(URL).with(comoAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "nome": "%s", "categoria": "%s" }
                                """.formatted(nome, categoria)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(resposta, "$.id");
    }

    // --- seguranca ---

    @Test
    @DisplayName("POST sem login -> 401")
    void criarSemLogin() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"nome\": \"Java\", \"categoria\": \"LINGUAGEM\" }"))
                .andExpect(status().isUnauthorized());
    }

    // --- caminho feliz ---

    @Test
    @DisplayName("POST completo -> 201 com Location")
    void criarCompleta() throws Exception {
        mockMvc.perform(post(URL).with(comoAdmin()).contentType(MediaType.APPLICATION_JSON).content("""
                        { "nome": "Java", "categoria": "LINGUAGEM",
                          "iconUrl": "https://exemplo.com/java.svg", "cor": "#E76F00" }
                        """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString(URL + "/")))
                .andExpect(jsonPath("$.nome").value("Java"))
                .andExpect(jsonPath("$.categoria").value("LINGUAGEM"))
                .andExpect(jsonPath("$.cor").value("#E76F00"));
    }

    @Test
    @DisplayName("Soft skill sem icone e sem cor e aceita (campos opcionais)")
    void criarSemOpcionais() throws Exception {
        mockMvc.perform(post(URL).with(comoAdmin()).contentType(MediaType.APPLICATION_JSON).content("""
                        { "nome": "Comunicação clara", "categoria": "SOFT_SKILL" }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Comunicação clara"))
                .andExpect(jsonPath("$.iconUrl").doesNotExist())
                .andExpect(jsonPath("$.cor").doesNotExist());
    }

    @Test
    @DisplayName("GET sem filtro: agrupado por categoria e em ordem alfabetica dentro dela")
    void listarOrdenado() throws Exception {
        criar("Java", "LINGUAGEM");
        criar("Docker", "FERRAMENTA");
        criar("Dart", "LINGUAGEM");

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].nome", contains("Docker", "Dart", "Java")));
    }

    @Test
    @DisplayName("GET ?categoria filtra o grupo")
    void filtrarPorCategoria() throws Exception {
        criar("Java", "LINGUAGEM");
        criar("Docker", "FERRAMENTA");
        criar("Dart", "LINGUAGEM");

        mockMvc.perform(get(URL).param("categoria", "LINGUAGEM"))
                .andExpect(jsonPath("$[*].nome", contains("Dart", "Java")));
    }

    @Test
    @DisplayName("PUT atualiza os campos")
    void atualizar() throws Exception {
        int id = criar("Docker", "FERRAMENTA");

        mockMvc.perform(put(URL + "/" + id).with(comoAdmin()).contentType(MediaType.APPLICATION_JSON).content("""
                        { "nome": "Docker / Compose", "categoria": "FERRAMENTA", "cor": "#2496ED" }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Docker / Compose"))
                .andExpect(jsonPath("$.cor").value("#2496ED"));

        // Confere que foi mesmo gravado (dirty checking + commit), e nao so devolvido na resposta
        mockMvc.perform(get(URL + "/" + id))
                .andExpect(jsonPath("$.nome").value("Docker / Compose"));
    }

    @Test
    @DisplayName("DELETE -> 204; repetir o DELETE -> 404")
    void deletar() throws Exception {
        int id = criar("Docker", "FERRAMENTA");

        mockMvc.perform(delete(URL + "/" + id).with(comoAdmin()))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete(URL + "/" + id).with(comoAdmin()))
                .andExpect(status().isNotFound());
    }

    // --- erros ---

    @Test
    @DisplayName("GET de id inexistente -> 404")
    void buscarInexistente() throws Exception {
        mockMvc.perform(get(URL + "/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Competencia nao encontrada com o ID: 9999"));
    }

    @Test
    @DisplayName("Nome vazio e categoria ausente -> 400 com erros por campo")
    void validacaoObrigatorios() throws Exception {
        mockMvc.perform(post(URL).with(comoAdmin()).contentType(MediaType.APPLICATION_JSON).content("""
                        { "nome": "" }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.nome").exists())
                .andExpect(jsonPath("$.erros.categoria").exists());
    }

    @Test
    @DisplayName("Cor e URL fora do formato -> 400 com erros por campo")
    void validacaoFormatos() throws Exception {
        mockMvc.perform(post(URL).with(comoAdmin()).contentType(MediaType.APPLICATION_JSON).content("""
                        { "nome": "X", "categoria": "TECNICA", "cor": "vermelho", "iconUrl": "nao-e-url" }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.cor").value("deve estar no formato #RRGGBB"))
                .andExpect(jsonPath("$.erros.iconUrl").exists());
    }

    @Test
    @DisplayName("Categoria inexistente no JSON -> 400 listando as aceitas")
    void categoriaInvalida() throws Exception {
        mockMvc.perform(post(URL).with(comoAdmin()).contentType(MediaType.APPLICATION_JSON).content("""
                        { "nome": "Spring", "categoria": "FRAMEWORK" }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.categoria",
                        containsString("[LINGUAGEM, FERRAMENTA, TECNICA, SOFT_SKILL]")));
    }

    @Test
    @DisplayName("JSON quebrado -> 400")
    void jsonQuebrado() throws Exception {
        mockMvc.perform(post(URL).with(comoAdmin()).contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"nome\": \"Java\","))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("JSON invalido ou mal formatado"));
    }
}
