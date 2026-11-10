package dev.gabriel.portfolio_api.dto;

import java.time.Instant;

/**
 * Resposta da primeira etapa do login: ainda NAO ha token, so o aviso de que o codigo foi enviado.
 *
 * @param emailMascarado ex.: "g*****@gmail.com", para o painel mostrar para onde foi o codigo
 *                       sem expor o endereco inteiro
 * @param expiraEm       ate quando o codigo vale (UTC), para o painel mostrar um contador
 */
public record LoginResponse(String mensagem, String emailMascarado, Instant expiraEm) {
}
