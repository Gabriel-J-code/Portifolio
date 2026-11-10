package dev.gabriel.portfolio_api.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.gabriel.portfolio_api.dto.DadosGeraisRequest;
import dev.gabriel.portfolio_api.dto.DadosGeraisResponse;
import dev.gabriel.portfolio_api.services.DadosGeraisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Recurso "singleton": a URL nao tem {id}, porque so existe um.
 * Por isso so ha GET e PUT (sem POST para criar, sem DELETE).
 */
@RestController
@RequestMapping("/api/dados-gerais")
@Tag(name = "Dados Gerais", description = "Secao 'Sobre': apresentacao, selo de disponibilidade e foto")
public class DadosGeraisController {

    private final DadosGeraisService dadosGeraisService;

    public DadosGeraisController(DadosGeraisService dadosGeraisService) {
        this.dadosGeraisService = dadosGeraisService;
    }

    @GetMapping
    @Operation(summary = "Dados da secao 'Sobre'", description = "404 enquanto nao forem configurados pelo painel.")
    public DadosGeraisResponse buscar() {
        return dadosGeraisService.buscar();
    }

    @PutMapping
    @Operation(summary = "Define os dados da secao 'Sobre'",
            description = "Cria na primeira chamada e substitui nas seguintes.",
            security = @SecurityRequirement(name = "admin"))
    public DadosGeraisResponse salvar(@Valid @RequestBody DadosGeraisRequest dados) {
        return dadosGeraisService.salvar(dados);
    }
}
