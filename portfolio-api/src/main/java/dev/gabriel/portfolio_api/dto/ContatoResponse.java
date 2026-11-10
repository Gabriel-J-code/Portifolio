package dev.gabriel.portfolio_api.dto;

import dev.gabriel.portfolio_api.entities.Contato;
import dev.gabriel.portfolio_api.entities.TipoContato;

public record ContatoResponse(
        int id,
        TipoContato tipo,
        String valor,
        String iconUrl,
        Integer ordem) {

    public static ContatoResponse de(Contato c) {
        return new ContatoResponse(c.getId(), c.getTipo(), c.getValor(), c.getIconUrl(), c.getOrdem());
    }
}
