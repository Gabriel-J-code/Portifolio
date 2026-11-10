package dev.gabriel.portfolio_api.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.gabriel.portfolio_api.dto.DadosGeraisRequest;
import dev.gabriel.portfolio_api.dto.DadosGeraisResponse;
import dev.gabriel.portfolio_api.entities.DadosGerais;
import dev.gabriel.portfolio_api.exceptions.RecursoNaoEncontradoException;
import dev.gabriel.portfolio_api.repositories.DadosGeraisRepository;

/**
 * Registro unico: so existem "buscar" e "salvar". Nao ha listar, criar separado nem apagar.
 */
@Service
public class DadosGeraisService {

    private final DadosGeraisRepository dadosGeraisRepository;

    public DadosGeraisService(DadosGeraisRepository dadosGeraisRepository) {
        this.dadosGeraisRepository = dadosGeraisRepository;
    }

    /**
     * Leitura nao cria nada: um GET deve ser "seguro" (sem efeitos colaterais).
     * Antes da primeira configuracao pelo painel, responde 404.
     */
    @Transactional(readOnly = true)
    public DadosGeraisResponse buscar() {
        return dadosGeraisRepository.findById(DadosGerais.ID_UNICO)
                .map(DadosGeraisResponse::de)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Dados gerais ainda nao foram configurados"));
    }

    /**
     * "Upsert": atualiza o registro se ele existir, ou cria se for a primeira vez.
     * Chamar duas vezes com os mesmos dados da o mesmo resultado (idempotente), como pede o PUT.
     */
    @Transactional
    public DadosGeraisResponse salvar(DadosGeraisRequest dados) {
        DadosGerais registro = dadosGeraisRepository.findById(DadosGerais.ID_UNICO)
                .orElseGet(DadosGerais::new); // novo objeto ja nasce com id = 1

        registro.setTexto(dados.texto().trim());
        registro.setDisponibilidade(vazioComoNulo(dados.disponibilidade()));
        registro.setFotoUrl(vazioComoNulo(dados.fotoUrl()));

        return DadosGeraisResponse.de(dadosGeraisRepository.save(registro));
    }

    /** "" ou "   " vira null: o front so precisa checar null para esconder o selo/foto. */
    private String vazioComoNulo(String valor) {
        return (valor == null || valor.isBlank()) ? null : valor.trim();
    }
}
