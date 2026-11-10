package dev.gabriel.portfolio_api.controllers;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.gabriel.portfolio_api.dto.LoginRequest;
import dev.gabriel.portfolio_api.dto.LoginResponse;
import dev.gabriel.portfolio_api.services.AcessoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Rotas publicas de autenticacao (liberadas em /api/auth/** no SecurityConfig). */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticacao", description = "Login do painel em duas etapas: senha + codigo por e-mail")
public class AuthController {

    private final AcessoService acessoService;

    public AuthController(AcessoService acessoService) {
        this.acessoService = acessoService;
    }

    @PostMapping("/login")
    @Operation(summary = "Etapa 1: confere e-mail e senha e envia um codigo de 6 digitos por e-mail",
            description = "Nao devolve token: o token vem na etapa 2 (verify-2fa), com o codigo. "
                    + "E-mail ou senha errados -> 401 com a mesma mensagem nos dois casos.")
    public LoginResponse login(@Valid @RequestBody LoginRequest dados) {
        return acessoService.iniciarLogin(dados);
    }
}
