package dev.gabriel.portfolio_api.dto;

import java.util.List;

import dev.gabriel.portfolio_api.entities.Imagem;
import dev.gabriel.portfolio_api.entities.Projeto;
import dev.gabriel.portfolio_api.entities.Tag;
import dev.gabriel.portfolio_api.entities.TipoProjeto;

/**
 * O que a API devolve sobre um projeto (o "contrato" com o Flutter).
 * Se a entidade mudar (ex.: nova coluna interna), o JSON publico so muda se mudarmos aqui.
 */
public record ProjetoResponse(
        int id,
        String titulo,
        String descricao,
        TipoProjeto tipo,
        String papel,
        String desafios,
        String capaUrl,
        String githubUrl,
        String visualizacaoUrl,
        Integer anoInicio,
        Integer anoFim,
        String periodo,
        List<TagResponse> tags,
        List<ImagemResponse> imagens) {

    public record TagResponse(int id, String nome, String cor) {
        public static TagResponse de(Tag tag) {
            return new TagResponse(tag.getId(), tag.getNome(), tag.getCor());
        }
    }

    public record ImagemResponse(int id, String url, String descricao) {
        public static ImagemResponse de(Imagem imagem) {
            return new ImagemResponse(imagem.getId(), imagem.getUrl(), imagem.getDescricao());
        }
    }

    /** Converte a entidade em DTO. Deve ser chamado dentro da transacao (tags/imagens sao LAZY). */
    public static ProjetoResponse de(Projeto p) {
        return new ProjetoResponse(
                p.getId(),
                p.getTitulo(),
                p.getDescricao(),
                p.getTipo(),
                p.getPapel(),
                p.getDesafios(),
                p.getCapaUrl(),
                p.getGithubUrl(),
                p.getVisualizacaoUrl(),
                p.getAnoInicio(),
                p.getAnoFim(),
                p.getPeriodo(),
                p.getTags().stream().map(TagResponse::de).toList(),
                p.getImagens().stream().map(ImagemResponse::de).toList());
    }
}
