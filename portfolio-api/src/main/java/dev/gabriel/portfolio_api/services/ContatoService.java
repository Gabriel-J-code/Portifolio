package dev.gabriel.portfolio_api.services;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.gabriel.portfolio_api.dto.ContatoRequest;
import dev.gabriel.portfolio_api.dto.ContatoResponse;
import dev.gabriel.portfolio_api.entities.Contato;
import dev.gabriel.portfolio_api.exceptions.RecursoNaoEncontradoException;
import dev.gabriel.portfolio_api.repositories.ContatoRepository;

@Service
public class ContatoService {

    private final ContatoRepository contatoRepository;

    public ContatoService(ContatoRepository contatoRepository) {
        this.contatoRepository = contatoRepository;
    }

    /** Todos os contatos, na ordem de exibicao do site. */
    @Transactional(readOnly = true)
    public List<ContatoResponse> listar() {
        return contatoRepository.findAllByOrderByOrdemAscIdAsc().stream().map(ContatoResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public ContatoResponse buscarPorId(int id) {
        return ContatoResponse.de(buscarEntidade(id));
    }

    @Transactional
    public ContatoResponse salvar(ContatoRequest dados) {
        Contato contato = new Contato();
        aplicarDados(contato, dados);
        return ContatoResponse.de(contatoRepository.save(contato));
    }

    @Transactional
    public ContatoResponse atualizar(int id, ContatoRequest dados) {
        Contato contato = buscarEntidade(id);
        aplicarDados(contato, dados); // dirty checking gera o UPDATE no commit
        return ContatoResponse.de(contato);
    }

    @Transactional
    public void deletar(int id) {
        contatoRepository.delete(buscarEntidade(id));
    }

    // --- auxiliares ---

    private Contato buscarEntidade(int id) {
        return contatoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Contato nao encontrado com o ID: " + id));
    }

    private void aplicarDados(Contato contato, ContatoRequest dados) {
        contato.setTipo(dados.tipo());
        contato.setValor(dados.valor().trim());
        contato.setIconUrl(dados.iconUrl());
        contato.setOrdem(dados.ordem());
    }
}
