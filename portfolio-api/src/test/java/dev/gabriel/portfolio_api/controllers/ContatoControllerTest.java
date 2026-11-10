package dev.gabriel.portfolio_api.controllers;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
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
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import com.jayway.jsonpath.JsonPath;

import dev.gabriel.portfolio_api.IntegracaoTest;
import dev.gabriel.portfolio_api.repositories.ContatoRepository;

class ContatoControllerTest extends IntegracaoTest {

    private static final String URL = "/api/contatos";

    @Autowired
    private ContatoRepository contatoRepository;

    @BeforeEach
    void limparBanco() {
        contatoRepository.deleteAll();
    }

    private String json(String tipo, String valor, int ordem) {
        return """
                { "tipo": "%s", "valor": "%s", "ordem": %d }
                """.formatted(tipo, valor, ordem);
    }

    private int criar(String tipo, String valor, int ordem) throws Exception {
        String resposta = mockMvc.perform(post(URL).with(comoAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content(json(tipo, valor, ordem)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(resposta, "$.id");
    }

    // --- seguranca ---

    @Test
    @DisplayName("POST sem login -> 401")
    void criarSemLogin() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content(json("GITHUB", "https://github.com/Gabriel-J-code", 1)))
                .andExpect(status().isUnauthorized());
    }

    // --- caminho feliz ---

    @Test
    @DisplayName("POST valido -> 201 com Location")
    void criar() throws Exception {
        mockMvc.perform(post(URL).with(comoAdmin()).contentType(MediaType.APPLICATION_JSON).content("""
                        { "tipo": "LINKEDIN", "valor": "https://www.linkedin.com/in/exemplo",
                          "iconUrl": "https://exemplo.com/linkedin.svg", "ordem": 1 }
                        """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString(URL + "/")))
                .andExpect(jsonPath("$.tipo").value("LINKEDIN"))
                .andExpect(jsonPath("$.valor").value("https://www.linkedin.com/in/exemplo"))
                .andExpect(jsonPath("$.ordem").value(1));
    }

    @Test
    @DisplayName("GET lista pela ordem definida, nao pela ordem de cadastro")
    void listarPelaOrdem() throws Exception {
        criar("GITHUB", "https://github.com/exemplo", 3);
        criar("LINKEDIN", "https://www.linkedin.com/in/exemplo", 1);
        criar("EMAIL", "exemplo@gmail.com", 2);

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].tipo", contains("LINKEDIN", "EMAIL", "GITHUB")));
    }

    @Test
    @DisplayName("PUT muda a ordem e a lista reflete")
    void atualizarOrdem() throws Exception {
        int github = criar("GITHUB", "https://github.com/exemplo", 2);
        criar("LINKEDIN", "https://www.linkedin.com/in/exemplo", 1);

        mockMvc.perform(put(URL + "/" + github).with(comoAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content(json("GITHUB", "https://github.com/exemplo", 1)))
                .andExpect(status().isOk());
        // Empate na ordem (ambos 1): desempata pelo id, ou seja, quem foi cadastrado primeiro
        mockMvc.perform(get(URL))
                .andExpect(jsonPath("$[*].tipo", contains("GITHUB", "LINKEDIN")));
    }

    @Test
    @DisplayName("DELETE -> 204; depois GET -> 404")
    void deletar() throws Exception {
        int id = criar("CURRICULO", "https://rxresu.me/exemplo/curriculo", 4);

        mockMvc.perform(delete(URL + "/" + id).with(comoAdmin()))
                .andExpect(status().isNoContent());
        mockMvc.perform(get(URL + "/" + id))
                .andExpect(status().isNotFound());
    }

    // --- validacao do valor conforme o tipo (@ValorCompativelComTipo) ---

    /**
     * @ParameterizedTest roda o mesmo teste uma vez para cada linha do @CsvSource.
     * Bom para regras com varias combinacoes de entrada.
     */
    @ParameterizedTest(name = "{0} com \"{1}\" e aceito")
    @CsvSource({
            "EMAIL,     gabriel.jesus@gmail.com",
            "LINKEDIN,  https://www.linkedin.com/in/exemplo",
            "GITHUB,    https://github.com/Gabriel-J-code",
            "CURRICULO, http://rxresu.me/exemplo/curriculo"
    })
    void valorCompativel(String tipo, String valor) throws Exception {
        mockMvc.perform(post(URL).with(comoAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content(json(tipo, valor, 1)))
                .andExpect(status().isCreated());
    }

    @ParameterizedTest(name = "{0} com \"{1}\" -> 400")
    @CsvSource({
            "EMAIL,     https://github.com/exemplo",   // link onde se espera e-mail
            "EMAIL,     gabriel.gmail.com",            // sem @
            "EMAIL,     gabriel @gmail.com",           // com espaco
            "GITHUB,    gabriel@gmail.com",            // e-mail onde se espera link
            "GITHUB,    Gabriel-J-code",               // so o usuario, sem link
            "LINKEDIN,  ftp://linkedin.com/in/exemplo" // esquema que nao e http(s)
    })
    void valorIncompativel(String tipo, String valor) throws Exception {
        mockMvc.perform(post(URL).with(comoAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content(json(tipo, valor, 1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.valor", containsString("para o tipo " + tipo)));
    }

    // --- outros erros ---

    @Test
    @DisplayName("Campos obrigatorios ausentes -> 400 sem erro duplicado de formato")
    void obrigatorios() throws Exception {
        mockMvc.perform(post(URL).with(comoAdmin()).contentType(MediaType.APPLICATION_JSON).content("""
                        { "valor": "" }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.tipo").exists())
                .andExpect(jsonPath("$.erros.ordem").exists())
                // so a mensagem do @NotBlank, nao a do validador de formato
                .andExpect(jsonPath("$.erros.valor", not(containsString("para o tipo"))));
    }

    @Test
    @DisplayName("Ordem menor que 1 -> 400")
    void ordemInvalida() throws Exception {
        mockMvc.perform(post(URL).with(comoAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content(json("GITHUB", "https://github.com/x", 0)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.ordem").exists());
    }

    @Test
    @DisplayName("Tipo inexistente (ex.: WHATSAPP) -> 400 listando os aceitos")
    void tipoInvalido() throws Exception {
        mockMvc.perform(post(URL).with(comoAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content(json("WHATSAPP", "https://wa.me/123", 1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.tipo", containsString("[LINKEDIN, GITHUB, EMAIL, CURRICULO]")));
    }

    @Test
    @DisplayName("GET de id inexistente -> 404")
    void buscarInexistente() throws Exception {
        mockMvc.perform(get(URL + "/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Contato nao encontrado com o ID: 9999"));
    }
}
