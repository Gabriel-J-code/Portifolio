package dev.gabriel.portfolio_api.exceptions;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

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
}
