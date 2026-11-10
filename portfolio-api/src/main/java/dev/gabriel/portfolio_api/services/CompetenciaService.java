package dev.gabriel.portfolio_api.services;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.gabriel.portfolio_api.dto.CompetenciaRequest;
import dev.gabriel.portfolio_api.dto.CompetenciaResponse;
import dev.gabriel.portfolio_api.entities.CategoriaCompetencia;
import dev.gabriel.portfolio_api.entities.Competencia;
import dev.gabriel.portfolio_api.exceptions.RecursoNaoEncontradoException;
import dev.gabriel.portfolio_api.repositories.CompetenciaRepository;

/**
 * Mesmo padrao do ProjetoService, mais simples: Competencia nao tem relacionamentos,
 * entao nao ha tags/imagens para carregar nem colecoes LAZY para se preocupar.
 */
@Service
public class CompetenciaService {

    private final CompetenciaRepository competenciaRepository;

    public CompetenciaService(CompetenciaRepository competenciaRepository) {
        this.competenciaRepository = competenciaRepository;
    }

    /** Todas (agrupadas por categoria), ou apenas as de uma categoria. */
    @Transactional(readOnly = true)
    public List<CompetenciaResponse> listar(CategoriaCompetencia categoria) {
        List<Competencia> competencias = (categoria == null)
                ? competenciaRepository.findAllByOrderByCategoriaAscNomeAsc()
                : competenciaRepository.findByCategoriaOrderByNomeAsc(categoria);
        return competencias.stream().map(CompetenciaResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public CompetenciaResponse buscarPorId(int id) {
        return CompetenciaResponse.de(buscarEntidade(id));
    }

    @Transactional
    public CompetenciaResponse salvar(CompetenciaRequest dados) {
        Competencia competencia = new Competencia();
        aplicarDados(competencia, dados);
        return CompetenciaResponse.de(competenciaRepository.save(competencia));
    }

    @Transactional
    public CompetenciaResponse atualizar(int id, CompetenciaRequest dados) {
        Competencia competencia = buscarEntidade(id);
        aplicarDados(competencia, dados);
        // Dirty checking: o UPDATE e gerado no commit da transacao, sem precisar de save().
        // (Aqui nao precisa de flush(): nao ha registros novos esperando ID.)
        return CompetenciaResponse.de(competencia);
    }

    @Transactional
    public void deletar(int id) {
        competenciaRepository.delete(buscarEntidade(id));
    }

    // --- auxiliares ---

    private Competencia buscarEntidade(int id) {
        return competenciaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Competencia nao encontrada com o ID: " + id));
    }

    private void aplicarDados(Competencia competencia, CompetenciaRequest dados) {
        competencia.setNome(dados.nome());
        competencia.setCategoria(dados.categoria());
        competencia.setIconUrl(dados.iconUrl());
        competencia.setCor(dados.cor());
    }
}
