package dev.gabriel.portfolio_api.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Teste UNITARIO (sem Spring, sem banco): getPeriodo() e so logica em Java,
 * entao basta criar o objeto e chamar o metodo. Roda em milissegundos.
 */
class ProjetoTest {

    @ParameterizedTest(name = "{0} a {1} -> \"{2}\"")
    @CsvSource(nullValues = "null", value = {
            "2023, null, 2023 – atual", // sem ano de fim: em andamento
            "2024, 2024, 2024",         // comecou e terminou no mesmo ano
            "2022, 2024, 2022 – 2024"   // varios anos
    })
    @DisplayName("Periodo exibido conforme os anos de inicio e fim")
    void periodo(Integer anoInicio, Integer anoFim, String esperado) {
        Projeto projeto = new Projeto("Titulo", "Descricao", TipoProjeto.ACADEMICO, anoInicio, anoFim);

        assertEquals(esperado, projeto.getPeriodo());
    }

    @Test
    @DisplayName("Sem ano de inicio (projeto ainda nao preenchido) -> periodo nulo")
    void semAnoInicio() {
        assertNull(new Projeto().getPeriodo());
    }
}
