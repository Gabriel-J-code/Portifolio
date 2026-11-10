package dev.gabriel.portfolio_api.controllers;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;

import com.jayway.jsonpath.JsonPath;

import dev.gabriel.portfolio_api.IntegracaoTest;
import dev.gabriel.portfolio_api.entities.Tag;
import dev.gabriel.portfolio_api.repositories.ProjetoRepository;
import dev.gabriel.portfolio_api.repositories.TagRepository;

class TagControllerTest extends IntegracaoTest {

    private static final String URL = "/api/tags";

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private ProjetoRepository projetoRepository;

    @BeforeEach
    void limparBanco() {
        projetoRepository.deleteAll(); // antes das tags, por causa da tabela projeto_tag
        tagRepository.deleteAll();
    }

    private String json(String nome, String cor) {
        return """
                { "nome": "%s", "cor": "%s" }
                """.formatted(nome, cor);
    }

    private int criar(String nome, String cor) throws Exception {
        String resposta = mockMvc.perform(post(URL).with(comoAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content(json(nome, cor)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(resposta, "$.id");
    }

    /** Cria um projeto usando a tag, pela propria API de projetos. */
    private int criarProjetoComTag(String titulo, int tagId) throws Exception {
        String resposta = mockMvc.perform(post("/api/projetos").with(comoAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                { "titulo": "%s", "descricao": "x", "tipo": "ACADEMICO",
                                  "anoInicio": 2024, "tagIds": [%d] }
                                """.formatted(titulo, tagId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(resposta, "$.id");
    }

    // --- seguranca ---

    @Test
    @DisplayName("POST sem login -> 401")
    void criarSemLogin() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(json("Java", "#E76F00")))
                .andExpect(status().isUnauthorized());
    }

    // --- caminho feliz ---

    @Test
    @DisplayName("POST valido -> 201 com Location")
    void criarTag() throws Exception {
        mockMvc.perform(post(URL).with(comoAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content(json("Spring Boot", "#6DB33F")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString(URL + "/")))
                .andExpect(jsonPath("$.nome").value("Spring Boot"))
                .andExpect(jsonPath("$.cor").value("#6DB33F"));
    }

    @Test
    @DisplayName("Espacos extras no nome sao removidos")
    void normalizaNome() throws Exception {
        mockMvc.perform(post(URL).with(comoAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content(json("  Spring    Boot ", "#6DB33F")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Spring Boot"));
    }

    @Test
    @DisplayName("GET lista em ordem alfabetica")
    void listarOrdenado() throws Exception {
        criar("Spring Boot", "#6DB33F");
        criar("Docker", "#2496ED");
        criar("Java", "#E76F00");

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].nome", contains("Docker", "Java", "Spring Boot")));
    }

    @Test
    @DisplayName("PUT na tag reflete nos projetos que a usam")
    void atualizarRefleteNosProjetos() throws Exception {
        int tag = criar("Sprng", "#6DB33F"); // nome com erro de digitacao
        int projeto = criarProjetoComTag("Portfolio", tag);

        mockMvc.perform(put(URL + "/" + tag).with(comoAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content(json("Spring", "#6DB33F")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Spring"));

        mockMvc.perform(get("/api/projetos/" + projeto))
                .andExpect(jsonPath("$.tags[0].nome").value("Spring"));
    }

    // --- nome unico (sem diferenciar maiusculas/minusculas) ---

    @ParameterizedTest(name = "\"{0}\" conflita com \"Java\" -> 409")
    @ValueSource(strings = { "Java", "java", "JAVA", "  Java  " })
    void nomeDuplicado(String nome) throws Exception {
        criar("Java", "#E76F00");

        mockMvc.perform(post(URL).with(comoAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content(json(nome, "#000000")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail", containsString("Ja existe uma tag com o nome")));
    }

    @Test
    @DisplayName("PUT renomeando para o nome de OUTRA tag -> 409")
    void renomearParaNomeExistente() throws Exception {
        criar("Java", "#E76F00");
        int docker = criar("Docker", "#2496ED");

        mockMvc.perform(put(URL + "/" + docker).with(comoAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content(json("java", "#2496ED")))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("PUT mudando so maiusculas/minusculas da PROPRIA tag -> 200")
    void corrigirCaixaDaPropriaTag() throws Exception {
        int id = criar("java", "#E76F00");

        mockMvc.perform(put(URL + "/" + id).with(comoAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content(json("Java", "#E76F00")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Java"));
    }

    @Test
    @DisplayName("O proprio SQL Server trata 'JAVA' e 'Java' como iguais na restricao UNIQUE")
    void bancoIgnoraMaiusculas() {
        tagRepository.saveAndFlush(new Tag(0, "Java", "#E76F00"));

        assertThrows(DataIntegrityViolationException.class,
                () -> tagRepository.saveAndFlush(new Tag(0, "JAVA", "#000000")));
    }

    // --- apagar ---

    @Test
    @DisplayName("DELETE de tag sem uso -> 204")
    void deletarSemUso() throws Exception {
        int id = criar("Java", "#E76F00");

        mockMvc.perform(delete(URL + "/" + id).with(comoAdmin()))
                .andExpect(status().isNoContent());
        mockMvc.perform(get(URL + "/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE de tag em uso -> 409 dizendo quantos projetos, e a tag continua existindo")
    void deletarEmUso() throws Exception {
        int id = criar("Java", "#E76F00");
        criarProjetoComTag("Projeto A", id);
        criarProjetoComTag("Projeto B", id);

        mockMvc.perform(delete(URL + "/" + id).with(comoAdmin()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(
                        "A tag 'Java' esta em uso por 2 projeto(s); remova-a dos projetos antes de apagar"));

        mockMvc.perform(get(URL + "/" + id)).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Depois de remover a tag dos projetos, o DELETE funciona")
    void deletarDepoisDeDesvincular() throws Exception {
        int id = criar("Java", "#E76F00");
        int projeto = criarProjetoComTag("Projeto A", id);

        mockMvc.perform(put("/api/projetos/" + projeto).with(comoAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                { "titulo": "Projeto A", "descricao": "x", "tipo": "ACADEMICO",
                                  "anoInicio": 2024, "tagIds": [] }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(delete(URL + "/" + id).with(comoAdmin()))
                .andExpect(status().isNoContent());
    }

    // --- outros erros ---

    @Test
    @DisplayName("Nome vazio, nome longo demais e cor invalida -> 400")
    void validacao() throws Exception {
        mockMvc.perform(post(URL).with(comoAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content(json("", "verde")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.nome").exists())
                .andExpect(jsonPath("$.erros.cor").value("deve estar no formato #RRGGBB"));

        mockMvc.perform(post(URL).with(comoAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content(json("x".repeat(51), "#000000")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.nome").exists());
    }

    @Test
    @DisplayName("GET de id inexistente -> 404")
    void buscarInexistente() throws Exception {
        mockMvc.perform(get(URL + "/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Tag nao encontrada com o ID: 9999"));
    }
}
