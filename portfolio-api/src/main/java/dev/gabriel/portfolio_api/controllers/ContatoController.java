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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import dev.gabriel.portfolio_api.dto.ContatoRequest;
import dev.gabriel.portfolio_api.dto.ContatoResponse;
import dev.gabriel.portfolio_api.services.ContatoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/contatos")
@Tag(name = "Contatos", description = "LinkedIn, GitHub, e-mail e curriculo")
public class ContatoController {

    private final ContatoService contatoService;

    public ContatoController(ContatoService contatoService) {
        this.contatoService = contatoService;
    }

    @GetMapping
    @Operation(summary = "Lista os contatos na ordem de exibicao")
    public List<ContatoResponse> listar() {
        return contatoService.listar();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhes de um contato")
    public ContatoResponse buscarPorId(@PathVariable int id) {
        return contatoService.buscarPorId(id);
    }

    @PostMapping
    @Operation(summary = "Cadastra um contato",
            description = "Para EMAIL, 'valor' e o endereco de e-mail; para os demais tipos, um link http(s).",
            security = @SecurityRequirement(name = "admin"))
    public ResponseEntity<ContatoResponse> criar(@Valid @RequestBody ContatoRequest dados) {
        ContatoResponse criado = contatoService.salvar(dados);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(criado.id()).toUri();
        return ResponseEntity.created(location).body(criado);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um contato", security = @SecurityRequirement(name = "admin"))
    public ContatoResponse atualizar(@PathVariable int id, @Valid @RequestBody ContatoRequest dados) {
        return contatoService.atualizar(id, dados);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove um contato", security = @SecurityRequirement(name = "admin"))
    public ResponseEntity<Void> deletar(@PathVariable int id) {
        contatoService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
