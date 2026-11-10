package dev.gabriel.portfolio_api.validacao;

import dev.gabriel.portfolio_api.dto.ContatoRequest;
import dev.gabriel.portfolio_api.entities.TipoContato;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Regra: o "valor" do contato precisa estar no formato que o "tipo" exige
 * (EMAIL -> endereco de e-mail; LINKEDIN, GITHUB, CURRICULO -> link http/https).
 *
 * O erro e "pendurado" no campo "valor" (addPropertyNode), e nao no objeto inteiro.
 * Assim ele aparece em "erros.valor" na resposta 400, como os erros de @NotBlank,
 * e o painel consegue mostrar a mensagem embaixo do input certo.
 */
public class ValorCompativelComTipoValidator implements ConstraintValidator<ValorCompativelComTipo, ContatoRequest> {

    @Override
    public boolean isValid(ContatoRequest contato, ConstraintValidatorContext context) {
        if (contato == null || contato.tipo() == null || contato.valor() == null || contato.valor().isBlank()) {
            // Campos ausentes ja sao reportados por @NotNull/@NotBlank; nao duplicamos o erro
            return true;
        }

        TipoContato tipo = contato.tipo();
        if (tipo.aceita(contato.valor())) {
            return true;
        }

        String mensagem = switch (tipo.getFormato()) {
            case EMAIL -> "deve ser um endereco de e-mail valido para o tipo " + tipo;
            case URL -> "deve ser um link http(s) valido para o tipo " + tipo;
        };
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(mensagem)
                .addPropertyNode("valor")
                .addConstraintViolation();
        return false;
    }
}
