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

import dev.gabriel.portfolio_api.dto.TagRequest;
import dev.gabriel.portfolio_api.dto.TagResponse;
import dev.gabriel.portfolio_api.services.TagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/tags")
@Tag(name = "Tags", description = "Etiquetas de tecnologia usadas nos projetos")
public class TagController {

    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping
    @Operation(summary = "Lista as tags em ordem alfabetica")
    public List<TagResponse> listar() {
        return tagService.listar();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhes de uma tag")
    public TagResponse buscarPorId(@PathVariable int id) {
        return tagService.buscarPorId(id);
    }

    @PostMapping
    @Operation(summary = "Cadastra uma tag",
            description = "O nome e unico sem diferenciar maiusculas/minusculas: 'java' conflita com 'Java' (409).",
            security = @SecurityRequirement(name = "admin"))
    public ResponseEntity<TagResponse> criar(@Valid @RequestBody TagRequest dados) {
        TagResponse criada = tagService.salvar(dados);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(criada.id()).toUri();
        return ResponseEntity.created(location).body(criada);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza uma tag (reflete em todos os projetos que a usam)",
            security = @SecurityRequirement(name = "admin"))
    public TagResponse atualizar(@PathVariable int id, @Valid @RequestBody TagRequest dados) {
        return tagService.atualizar(id, dados);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove uma tag",
            description = "Recusado com 409 se a tag ainda estiver vinculada a algum projeto.",
            security = @SecurityRequirement(name = "admin"))
    public ResponseEntity<Void> deletar(@PathVariable int id) {
        tagService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
