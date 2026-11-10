package dev.gabriel.portfolio_api.exceptions;

/**
 * E-mail ou senha incorretos. Vira HTTP 401.
 *
 * A mensagem e a mesma nos dois casos de proposito: dizer "e-mail nao cadastrado"
 * permitiria a um atacante descobrir qual e-mail e o do painel ("enumeracao de usuarios").
 */
public class CredenciaisInvalidasException extends RuntimeException {
    public CredenciaisInvalidasException() {
        super("E-mail ou senha invalidos");
    }
}
