package dev.gabriel.portfolio_api.dto;

import dev.gabriel.portfolio_api.entities.DadosGerais;

/** Sem "id": como so existe um registro, o id nao interessa a quem consome a API. */
public record DadosGeraisResponse(
        String texto,
        String disponibilidade,
        String fotoUrl) {

    public static DadosGeraisResponse de(DadosGerais d) {
        return new DadosGeraisResponse(d.getTexto(), d.getDisponibilidade(), d.getFotoUrl());
    }
}
