package dev.gabriel.portfolio_api.controllers;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import dev.gabriel.portfolio_api.dto.ProjetoRequest;
import dev.gabriel.portfolio_api.dto.ProjetoResponse;
import dev.gabriel.portfolio_api.entities.TipoProjeto;
import dev.gabriel.portfolio_api.services.ProjetoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Camada HTTP: traduz requisicoes em chamadas ao service e escolhe o status da resposta.
 * Nao tem regra de negocio; erros sao tratados no GlobalExceptionHandler.
 */
@RestController
@RequestMapping("/api/projetos")
@Tag(name = "Projetos", description = "Carrosseis de projetos profissionais e academicos")
public class ProjetoController {

    private final ProjetoService projetoService;

    public ProjetoController(ProjetoService projetoService) {
        this.projetoService = projetoService;
    }

    @GetMapping
    @Operation(summary = "Lista projetos (mais recentes primeiro)",
            description = "Sem filtros: todos. Com 'tipo': um carrossel. Com 'tagId': projetos daquela tag.")
    public List<ProjetoResponse> listar(
            @Parameter(description = "Carrossel: PROFISSIONAL ou ACADEMICO")
            @RequestParam(required = false) TipoProjeto tipo,
            @Parameter(description = "Filtra pelos projetos que usam esta tag")
            @RequestParam(required = false) Integer tagId) {
        if (tagId != null) {
            return projetoService.listarPorTag(tagId);
        }
        return projetoService.listar(tipo);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhes de um projeto")
    public ProjetoResponse buscarPorId(@PathVariable int id) {
        return projetoService.buscarPorId(id);
    }

    @PostMapping
    @Operation(summary = "Cadastra um projeto", security = @SecurityRequirement(name = "admin"))
    public ResponseEntity<ProjetoResponse> criar(@Valid @RequestBody ProjetoRequest dados) {
        ProjetoResponse criado = projetoService.salvar(dados);
        // Padrao REST: 201 Created + cabecalho Location com o endereco do novo recurso
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(criado.id()).toUri();
        return ResponseEntity.created(location).body(criado);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um projeto (substitui todos os campos, tags e imagens)",
            security = @SecurityRequirement(name = "admin"))
    public ProjetoResponse atualizar(@PathVariable int id, @Valid @RequestBody ProjetoRequest dados) {
        return projetoService.atualizar(id, dados);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove um projeto (e suas imagens)", security = @SecurityRequirement(name = "admin"))
    public ResponseEntity<Void> deletar(@PathVariable int id) {
        projetoService.deletar(id);
        return ResponseEntity.noContent().build(); // 204: deu certo, nada para devolver
    }
}
