package dev.gabriel.portfolio_api.exceptions;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

// Spring Boot 4 usa o Jackson 3, cujo pacote mudou de com.fasterxml.jackson para tools.jackson
import tools.jackson.databind.exc.InvalidFormatException;

/**
 * Converte excecoes em respostas HTTP padronizadas, para TODOS os controllers.
 *
 * O formato e o ProblemDetail (RFC 9457), padrao do Spring 6+:
 * { "type": "about:blank", "title": "Not Found", "status": 404,
 *   "detail": "Projeto nao encontrado com o ID: 99", "instance": "/api/projetos/99" }
 *
 * Herdar de ResponseEntityExceptionHandler faz o Spring ja tratar no mesmo formato os erros
 * "de infraestrutura": JSON malformado, enum invalido, metodo HTTP errado, etc.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ProblemDetail tratarNaoEncontrado(RecursoNaoEncontradoException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(RegraDeNegocioException.class)
    public ProblemDetail tratarRegraDeNegocio(RegraDeNegocioException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(ConflitoException.class)
    public ProblemDetail tratarConflito(ConflitoException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    /**
     * Rede de seguranca: o BANCO recusou a operacao por violar uma restricao
     * (UNIQUE, NOT NULL, chave estrangeira...). Normalmente o service ja barra antes,
     * mas isto cobre o que escapar (ex.: duas requisicoes simultaneas).
     * A mensagem original traz SQL e nomes internos, entao nao e repassada ao cliente.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail tratarViolacaoDeIntegridade(DataIntegrityViolationException ex) {
        logger.warn("Violacao de integridade no banco", ex);
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "A operacao conflita com dados ja existentes");
    }

    /**
     * Falhas do @Valid. Acrescenta a lista de campos invalidos, para o painel
     * conseguir mostrar a mensagem embaixo de cada input:
     * "erros": { "titulo": "must not be blank", "anoInicio": "must not be null" }
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> erros = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(erro -> erros.putIfAbsent(erro.getField(), erro.getDefaultMessage()));

        ProblemDetail problema = ex.getBody();
        problema.setDetail("Um ou mais campos estao invalidos");
        problema.setProperty("erros", erros);
        return handleExceptionInternal(ex, problema, headers, status, request);
    }

    /**
     * JSON que nem chega a virar o DTO: sintaxe quebrada, ou um valor que nao cabe no tipo
     * (ex.: "categoria": "FRAMEWORK", que nao existe no enum). O padrao do Spring so diz
     * "Failed to read request"; aqui explicamos qual campo e, para enums, quais valores valem.
     */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, "JSON invalido ou mal formatado");

        if (ex.getCause() instanceof InvalidFormatException erro) {
            String campo = erro.getPath().stream()
                    .map(ref -> ref.getPropertyName())
                    .filter(nome -> nome != null)
                    .reduce((a, b) -> a + "." + b)
                    .orElse("?");
            String mensagem = "valor '" + erro.getValue() + "' invalido";
            if (erro.getTargetType() != null && erro.getTargetType().isEnum()) {
                mensagem += "; aceitos: " + Arrays.toString(erro.getTargetType().getEnumConstants());
            }
            problema.setDetail("Um ou mais campos estao invalidos");
            problema.setProperty("erros", Map.of(campo, mensagem));
        }
        return handleExceptionInternal(ex, problema, headers, status, request);
    }
}
