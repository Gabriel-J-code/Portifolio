package dev.gabriel.portfolio_api.dto;

import java.util.List;

import org.hibernate.validator.constraints.URL;

import dev.gabriel.portfolio_api.entities.TipoProjeto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * O que o painel administrativo envia para criar/atualizar um projeto.
 *
 * Por que nao receber a entidade Projeto direto?
 * - O cliente nao deve escolher "id" nem mexer em campos internos.
 * - Tags chegam so como ids (tagIds), sem precisar montar objetos Tag.
 * - As regras de validacao ficam aqui, separadas do mapeamento do banco.
 *
 * Exemplo de JSON:
 * {
 *   "titulo": "BeesCaatinga", "descricao": "...", "tipo": "PROFISSIONAL",
 *   "anoInicio": 2023, "anoFim": 2024,
 *   "tagIds": [1, 3],
 *   "imagens": [{ "url": "https://...", "descricao": "Tela inicial" }]
 * }
 */
public record ProjetoRequest(

        @NotBlank @Size(max = 255)
        String titulo,

        @NotBlank
        String descricao,

        @NotNull
        TipoProjeto tipo,

        String papel,

        String desafios,

        @URL @Size(max = 255)
        String capaUrl,

        @URL @Size(max = 255)
        String githubUrl,

        @URL @Size(max = 255)
        String visualizacaoUrl,

        @NotNull @Min(1990) @Max(2100)
        Integer anoInicio,

        @Min(1990) @Max(2100)
        Integer anoFim,

        List<Integer> tagIds,

        // @Valid faz a validacao "descer" para cada imagem da lista
        List<@Valid ImagemRequest> imagens) {

    public record ImagemRequest(
            @NotBlank @URL @Size(max = 255) String url,
            @Size(max = 255) String descricao) {
    }
}
