package dev.gabriel.portfolio_api.exceptions;

/**
 * Codigo do 2FA incorreto, expirado, ja usado ou com tentativas esgotadas. Vira HTTP 401.
 */
public class CodigoInvalidoException extends RuntimeException {
    public CodigoInvalidoException(String mensagem) {
        super(mensagem);
    }
}
