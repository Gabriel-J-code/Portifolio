package dev.gabriel.portfolio_api.exceptions;

/**
 * Lancada quando um registro buscado por ID nao existe.
 * Depois, um @RestControllerAdvice pode converte-la em HTTP 404.
 */
public class RecursoNaoEncontradoException extends RuntimeException {
    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
