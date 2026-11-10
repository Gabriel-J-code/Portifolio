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

import dev.gabriel.portfolio_api.dto.CompetenciaRequest;
import dev.gabriel.portfolio_api.dto.CompetenciaResponse;
import dev.gabriel.portfolio_api.entities.CategoriaCompetencia;
import dev.gabriel.portfolio_api.services.CompetenciaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/competencias")
@Tag(name = "Competencias", description = "Linguagens, ferramentas, tecnicas e soft skills")
public class CompetenciaController {

    private final CompetenciaService competenciaService;

    public CompetenciaController(CompetenciaService competenciaService) {
        this.competenciaService = competenciaService;
    }

    @GetMapping
    @Operation(summary = "Lista competencias",
            description = "Sem filtro: todas, ordenadas por categoria e nome. Com 'categoria': so aquele grupo.")
    public List<CompetenciaResponse> listar(
            @Parameter(description = "LINGUAGEM, FERRAMENTA, TECNICA ou SOFT_SKILL")
            @RequestParam(required = false) CategoriaCompetencia categoria) {
        return competenciaService.listar(categoria);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhes de uma competencia")
    public CompetenciaResponse buscarPorId(@PathVariable int id) {
        return competenciaService.buscarPorId(id);
    }

    @PostMapping
    @Operation(summary = "Cadastra uma competencia", security = @SecurityRequirement(name = "admin"))
    public ResponseEntity<CompetenciaResponse> criar(@Valid @RequestBody CompetenciaRequest dados) {
        CompetenciaResponse criada = competenciaService.salvar(dados);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(criada.id()).toUri();
        return ResponseEntity.created(location).body(criada);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza uma competencia", security = @SecurityRequirement(name = "admin"))
    public CompetenciaResponse atualizar(@PathVariable int id, @Valid @RequestBody CompetenciaRequest dados) {
        return competenciaService.atualizar(id, dados);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove uma competencia", security = @SecurityRequirement(name = "admin"))
    public ResponseEntity<Void> deletar(@PathVariable int id) {
        competenciaService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
