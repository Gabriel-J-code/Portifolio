package dev.gabriel.portfolio_api.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

import dev.gabriel.portfolio_api.IntegracaoTest;
import dev.gabriel.portfolio_api.repositories.DadosGeraisRepository;

class DadosGeraisControllerTest extends IntegracaoTest {

    private static final String URL = "/api/dados-gerais";

    @Autowired
    private DadosGeraisRepository dadosGeraisRepository;

    // Acesso direto ao banco via SQL, sem passar por entidade/repository
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void limparBanco() {
        dadosGeraisRepository.deleteAll();
    }

    private static final String DADOS = """
            {
              "texto": "Olá, meu nome é Gabriel de Jesus, sou desenvolvedor full stack.",
              "disponibilidade": "Disponível para contratação",
              "fotoUrl": "https://exemplo.com/foto.png"
            }
            """;

    private void salvar(String json) throws Exception {
        mockMvc.perform(put(URL).with(comoAdmin()).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk());
    }

    // --- antes de configurar ---

    @Test
    @DisplayName("GET antes da primeira configuracao -> 404 (e o GET nao cria nada)")
    void buscarSemConfigurar() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Dados gerais ainda nao foram configurados"));

        assertEquals(0, dadosGeraisRepository.count());
    }

    // --- seguranca ---

    @Test
    @DisplayName("PUT sem login -> 401")
    void salvarSemLogin() throws Exception {
        mockMvc.perform(put(URL).contentType(MediaType.APPLICATION_JSON).content(DADOS))
                .andExpect(status().isUnauthorized());
    }

    // --- registro unico ---

    @Test
    @DisplayName("Primeiro PUT cria o registro; GET publico passa a devolve-lo")
    void primeiroPutCria() throws Exception {
        mockMvc.perform(put(URL).with(comoAdmin()).contentType(MediaType.APPLICATION_JSON).content(DADOS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.texto").value("Olá, meu nome é Gabriel de Jesus, sou desenvolvedor full stack."))
                .andExpect(jsonPath("$.disponibilidade").value("Disponível para contratação"))
                .andExpect(jsonPath("$.id").doesNotExist()); // o id nao faz parte do contrato

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fotoUrl").value("https://exemplo.com/foto.png"));
    }

    @Test
    @DisplayName("PUTs seguintes substituem os dados, sem criar uma segunda linha")
    void putsSeguintesAtualizam() throws Exception {
        salvar(DADOS);
        salvar("""
                { "texto": "Texto novo", "disponibilidade": "Aberto a propostas" }
                """);

        mockMvc.perform(get(URL))
                .andExpect(jsonPath("$.texto").value("Texto novo"))
                .andExpect(jsonPath("$.disponibilidade").value("Aberto a propostas"))
                .andExpect(jsonPath("$.fotoUrl").doesNotExist()); // PUT substitui tudo: foto omitida = removida

        assertEquals(1, dadosGeraisRepository.count());
    }

    @Test
    @DisplayName("Disponibilidade vazia vira null (o site esconde o selo)")
    void disponibilidadeVazia() throws Exception {
        mockMvc.perform(put(URL).with(comoAdmin()).contentType(MediaType.APPLICATION_JSON).content("""
                        { "texto": "Olá", "disponibilidade": "   ", "fotoUrl": "" }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disponibilidade").doesNotExist())
                .andExpect(jsonPath("$.fotoUrl").doesNotExist());
    }

    @Test
    @DisplayName("O banco recusa uma segunda linha mesmo via SQL direto (check constraint id = 1)")
    void bancoRecusaSegundaLinha() {
        jdbcTemplate.update("INSERT INTO dados_gerais (id, texto) VALUES (1, 'primeira')");

        assertThrows(DataIntegrityViolationException.class,
                () -> jdbcTemplate.update("INSERT INTO dados_gerais (id, texto) VALUES (2, 'segunda')"));
    }

    @Test
    @DisplayName("Nao existe POST nem DELETE para um registro unico -> 405")
    void semPostNemDelete() throws Exception {
        mockMvc.perform(post(URL).with(comoAdmin()).contentType(MediaType.APPLICATION_JSON).content(DADOS))
                .andExpect(status().isMethodNotAllowed());
        mockMvc.perform(delete(URL).with(comoAdmin()))
                .andExpect(status().isMethodNotAllowed());
    }

    // --- validacao ---

    @Test
    @DisplayName("Texto vazio, selo longo demais e foto com URL invalida -> 400")
    void validacao() throws Exception {
        mockMvc.perform(put(URL).with(comoAdmin()).contentType(MediaType.APPLICATION_JSON).content("""
                        { "texto": "", "disponibilidade": "%s", "fotoUrl": "nao-e-url" }
                        """.formatted("x".repeat(101))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.texto").exists())
                .andExpect(jsonPath("$.erros.disponibilidade").exists())
                .andExpect(jsonPath("$.erros.fotoUrl").exists());

        assertEquals(0, dadosGeraisRepository.count());
    }
}
