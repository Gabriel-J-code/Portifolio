package dev.gabriel.portfolio_api.exceptions;

/**
 * Lancada quando os dados sao validos isoladamente, mas violam uma regra do dominio
 * (ex.: ano de fim anterior ao ano de inicio). Vira HTTP 400 no GlobalExceptionHandler.
 *
 * Por que nao usar IllegalArgumentException? Ela tambem e lancada por bugs dentro do
 * proprio Java/Spring; se o handler capturasse todas, um bug apareceria como "erro do cliente".
 */
public class RegraDeNegocioException extends RuntimeException {
    public RegraDeNegocioException(String mensagem) {
        super(mensagem);
    }
}
