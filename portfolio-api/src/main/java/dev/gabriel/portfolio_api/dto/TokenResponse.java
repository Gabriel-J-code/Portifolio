package dev.gabriel.portfolio_api.dto;

import java.time.Instant;

/**
 * Resposta da etapa 2 do login. O painel guarda o token e o envia nas requisicoes de escrita:
 *   Authorization: Bearer <token>
 *
 * @param tipo     sempre "Bearer" (o nome do esquema usado no cabecalho)
 * @param expiraEm ate quando o token vale; depois disso, e preciso logar de novo
 */
public record TokenResponse(String token, String tipo, Instant expiraEm) {
}
