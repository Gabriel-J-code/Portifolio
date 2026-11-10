package dev.gabriel.portfolio_api.exceptions;

/**
 * Lancada quando a operacao criaria um registro duplicado onde so pode haver um
 * (ex.: um segundo contato do tipo LINKEDIN). Vira HTTP 409 Conflict.
 *
 * Por que 409 e nao 400? O pedido em si esta bem formado e valido;
 * o problema e o conflito com o estado atual do banco.
 */
public class ConflitoException extends RuntimeException {
    public ConflitoException(String mensagem) {
        super(mensagem);
    }
}
