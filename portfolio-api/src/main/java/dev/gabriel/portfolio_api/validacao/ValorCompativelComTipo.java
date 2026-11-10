package dev.gabriel.portfolio_api.validacao;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Anotacao de validacao propria (Bean Validation), usada como @NotBlank ou @URL.
 *
 * Diferenca: e aplicada na CLASSE (no record ContatoRequest), nao em um campo,
 * porque a regra depende de dois campos ao mesmo tempo: o "valor" valido depende do "tipo".
 * Quem implementa a regra e o ValorCompativelComTipoValidator (ligado via validatedBy).
 */
@Documented
@Constraint(validatedBy = ValorCompativelComTipoValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValorCompativelComTipo {

    String message() default "valor incompativel com o tipo de contato";

    // Os dois atributos abaixo sao exigidos pela especificacao, mesmo sem uso aqui
    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
