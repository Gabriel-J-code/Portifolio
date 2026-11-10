package dev.gabriel.portfolio_api.dto;

import dev.gabriel.portfolio_api.entities.Tag;

/** Usado tanto em /api/tags quanto dentro de cada projeto (ProjetoResponse.tags). */
public record TagResponse(int id, String nome, String cor) {

    public static TagResponse de(Tag tag) {
        return new TagResponse(tag.getId(), tag.getNome(), tag.getCor());
    }
}
