package dev.gabriel.portfolio_api.dto;

import dev.gabriel.portfolio_api.entities.CategoriaCompetencia;
import dev.gabriel.portfolio_api.entities.Competencia;

public record CompetenciaResponse(
        int id,
        String nome,
        CategoriaCompetencia categoria,
        String iconUrl,
        String cor) {

    public static CompetenciaResponse de(Competencia c) {
        return new CompetenciaResponse(c.getId(), c.getNome(), c.getCategoria(), c.getIconUrl(), c.getCor());
    }
}
