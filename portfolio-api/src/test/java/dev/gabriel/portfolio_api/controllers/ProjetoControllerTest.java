package dev.gabriel.portfolio_api.controllers;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
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
import dev.gabriel.portfolio_api.entities.Tag;
import dev.gabriel.portfolio_api.repositories.ProjetoRepository;
import dev.gabriel.portfolio_api.repositories.TagRepository;

/**
 * Testes de integracao de /api/projetos: requisicao HTTP simulada -> seguranca -> controller
 * -> service -> SQL Server (container) -> resposta JSON.
 */
class ProjetoControllerTest extends IntegracaoTest {

    private static final String URL = "/api/projetos";

    @Autowired
    private ProjetoRepository projetoRepository;

    @Autowired
    private TagRepository tagRepository;

    private int tagJava;
    private int tagSpring;

    /**
     * Roda antes de CADA teste: limpa o banco e recria as tags.
     * Assim cada teste comeca do zero e nao depende da ordem de execucao.
     */
    @BeforeEach
    void prepararBanco() {
        projetoRepository.deleteAll(); // antes das tags, por causa da tabela projeto_tag
        tagRepository.deleteAll();
        tagJava = tagRepository.save(new Tag(0, "Java", "#E76F00")).getId();
        tagSpring = tagRepository.save(new Tag(0, "Spring Boot", "#6DB33F")).getId();
    }

    // --- dados de exemplo ---

    private String projetoValido() {
        return """
                {
                  "titulo": "BeesCaatinga",
                  "descricao": "Gestão de apiários da Caatinga",
                  "tipo": "PROFISSIONAL",
                  "papel": "Desenvolvedor front-end",
                  "desafios": "Sincronização offline",
                  "anoInicio": 2023,
                  "anoFim": 2024,
                  "tagIds": [%d, %d],
                  "imagens": [{ "url": "https://exemplo.com/tela1.png", "descricao": "Tela inicial" }]
                }
                """.formatted(tagJava, tagSpring);
    }

    /** Cria um projeto pela API e devolve o id gerado. */
    private int criarProjeto(String json) throws Exception {
        String resposta = mockMvc.perform(post(URL).with(comoAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(resposta, "$.id");
    }

    // --- seguranca ---

    @Test
    @DisplayName("GET e publico: lista sem precisar de login")
    void listarSemLogin() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("POST sem login -> 401")
    void criarSemLogin() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(projetoValido()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST com senha errada -> 401")
    void criarComSenhaErrada() throws Exception {
        mockMvc.perform(post(URL)
                        .with(httpBasic(ADMIN_USER, "errada"))
                        .contentType(MediaType.APPLICATION_JSON).content(projetoValido()))
                .andExpect(status().isUnauthorized());
    }

    // --- caminho feliz ---

    @Test
    @DisplayName("POST valido -> 201, Location e projeto completo com tags, imagens e periodo")
    void criar() throws Exception {
        mockMvc.perform(post(URL).with(comoAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content(projetoValido()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString(URL + "/")))
                .andExpect(jsonPath("$.titulo").value("BeesCaatinga"))
                .andExpect(jsonPath("$.descricao").value("Gestão de apiários da Caatinga")) // acentos/NVARCHAR
                .andExpect(jsonPath("$.periodo").value("2023 – 2024"))
                .andExpect(jsonPath("$.tags", hasSize(2)))
                .andExpect(jsonPath("$.imagens", hasSize(1)))
                .andExpect(jsonPath("$.imagens[0].id").value(not(0)));
    }

    @Test
    @DisplayName("GET /{id} devolve o projeto criado")
    void buscarPorId() throws Exception {
        int id = criarProjeto(projetoValido());

        mockMvc.perform(get(URL + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.tags[*].nome", hasSize(2)));
    }

    @Test
    @DisplayName("GET ?tipo filtra pelo carrossel")
    void filtrarPorTipo() throws Exception {
        criarProjeto(projetoValido()); // PROFISSIONAL

        mockMvc.perform(get(URL).param("tipo", "PROFISSIONAL"))
                .andExpect(jsonPath("$", hasSize(1)));
        mockMvc.perform(get(URL).param("tipo", "ACADEMICO"))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("GET ?tagId devolve so os projetos daquela tag")
    void filtrarPorTag() throws Exception {
        criarProjeto(projetoValido());
        criarProjeto("""
                { "titulo": "Outro", "descricao": "Sem tags", "tipo": "ACADEMICO", "anoInicio": 2022 }
                """);

        mockMvc.perform(get(URL).param("tagId", String.valueOf(tagSpring)))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].titulo").value("BeesCaatinga"));
    }

    @Test
    @DisplayName("GET lista os mais recentes primeiro")
    void listarOrdenadoPorAno() throws Exception {
        criarProjeto("""
                { "titulo": "Antigo", "descricao": "x", "tipo": "ACADEMICO", "anoInicio": 2020 }
                """);
        criarProjeto("""
                { "titulo": "Recente", "descricao": "x", "tipo": "ACADEMICO", "anoInicio": 2025 }
                """);

        mockMvc.perform(get(URL))
                .andExpect(jsonPath("$[0].titulo").value("Recente"))
                .andExpect(jsonPath("$[1].titulo").value("Antigo"));
    }

    @Test
    @DisplayName("PUT substitui campos, tags e imagens")
    void atualizar() throws Exception {
        int id = criarProjeto(projetoValido());

        mockMvc.perform(put(URL + "/" + id).with(comoAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {
                                  "titulo": "BeesCaatinga (editado)",
                                  "descricao": "Nova descrição",
                                  "tipo": "PROFISSIONAL",
                                  "anoInicio": 2023,
                                  "tagIds": [%d],
                                  "imagens": [
                                    { "url": "https://exemplo.com/tela2.png" },
                                    { "url": "https://exemplo.com/tela3.png" }
                                  ]
                                }
                                """.formatted(tagJava)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("BeesCaatinga (editado)"))
                .andExpect(jsonPath("$.periodo").value("2023")) // sem anoFim -> so um ano
                .andExpect(jsonPath("$.tags", hasSize(1)))
                .andExpect(jsonPath("$.tags[0].nome").value("Java"))
                .andExpect(jsonPath("$.imagens", hasSize(2)))
                .andExpect(jsonPath("$.imagens[0].id").value(not(0))); // flush() gerou os IDs
    }

    @Test
    @DisplayName("DELETE -> 204, e depois o projeto nao existe mais")
    void deletar() throws Exception {
        int id = criarProjeto(projetoValido());

        mockMvc.perform(delete(URL + "/" + id).with(comoAdmin()))
                .andExpect(status().isNoContent());
        mockMvc.perform(get(URL + "/" + id))
                .andExpect(status().isNotFound());
    }

    // --- erros ---

    @Test
    @DisplayName("GET de id inexistente -> 404 com ProblemDetail")
    void buscarInexistente() throws Exception {
        mockMvc.perform(get(URL + "/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Projeto nao encontrado com o ID: 9999"));
    }

    @Test
    @DisplayName("PUT e DELETE de id inexistente -> 404")
    void alterarInexistente() throws Exception {
        mockMvc.perform(put(URL + "/9999").with(comoAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content(projetoValido()))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete(URL + "/9999").with(comoAdmin()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Campos invalidos -> 400 com a lista de erros por campo")
    void validacao() throws Exception {
        mockMvc.perform(post(URL).with(comoAdmin()).contentType(MediaType.APPLICATION_JSON).content("""
                        { "titulo": "", "githubUrl": "nao-e-url", "anoInicio": 1500 }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.titulo").exists())
                .andExpect(jsonPath("$.erros.descricao").exists())
                .andExpect(jsonPath("$.erros.tipo").exists())
                .andExpect(jsonPath("$.erros.githubUrl").exists())
                .andExpect(jsonPath("$.erros.anoInicio").exists());
    }

    @Test
    @DisplayName("anoFim antes de anoInicio -> 400 (regra de negocio)")
    void periodoInvalido() throws Exception {
        mockMvc.perform(post(URL).with(comoAdmin()).contentType(MediaType.APPLICATION_JSON).content("""
                        { "titulo": "X", "descricao": "Y", "tipo": "ACADEMICO", "anoInicio": 2024, "anoFim": 2020 }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("O ano de fim nao pode ser anterior ao ano de inicio"));
    }

    @Test
    @DisplayName("Tag inexistente -> 404 e nada e salvo")
    void tagInexistente() throws Exception {
        mockMvc.perform(post(URL).with(comoAdmin()).contentType(MediaType.APPLICATION_JSON).content("""
                        { "titulo": "X", "descricao": "Y", "tipo": "ACADEMICO", "anoInicio": 2024, "tagIds": [99999] }
                        """))
                .andExpect(status().isNotFound());

        mockMvc.perform(get(URL)).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("Tipo inexistente no JSON -> 400 listando os valores aceitos")
    void tipoInvalidoNoCorpo() throws Exception {
        mockMvc.perform(post(URL).with(comoAdmin()).contentType(MediaType.APPLICATION_JSON).content("""
                        { "titulo": "X", "descricao": "Y", "tipo": "PESSOAL", "anoInicio": 2024 }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.tipo", endsWith("[PROFISSIONAL, ACADEMICO]")));
    }

    @Test
    @DisplayName("Tipo inexistente no filtro (?tipo=) -> 400")
    void tipoInvalidoNoFiltro() throws Exception {
        mockMvc.perform(get(URL).param("tipo", "INVALIDO"))
                .andExpect(status().isBadRequest());
    }
}
