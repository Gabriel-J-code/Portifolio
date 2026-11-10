package dev.gabriel.portfolio_api.services;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.gabriel.portfolio_api.dto.TagRequest;
import dev.gabriel.portfolio_api.dto.TagResponse;
import dev.gabriel.portfolio_api.entities.Tag;
import dev.gabriel.portfolio_api.exceptions.ConflitoException;
import dev.gabriel.portfolio_api.exceptions.RecursoNaoEncontradoException;
import dev.gabriel.portfolio_api.repositories.ProjetoRepository;
import dev.gabriel.portfolio_api.repositories.TagRepository;

/**
 * Duas regras proprias de Tag:
 * 1. Nome unico, sem diferenciar maiusculas/minusculas e ignorando espacos nas pontas.
 * 2. Tag em uso por algum projeto nao pode ser apagada (409): primeiro remova-a dos projetos.
 */
@Service
public class TagService {

    private final TagRepository tagRepository;
    private final ProjetoRepository projetoRepository;

    public TagService(TagRepository tagRepository, ProjetoRepository projetoRepository) {
        this.tagRepository = tagRepository;
        this.projetoRepository = projetoRepository;
    }

    @Transactional(readOnly = true)
    public List<TagResponse> listar() {
        return tagRepository.findAllByOrderByNomeAsc().stream().map(TagResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public TagResponse buscarPorId(int id) {
        return TagResponse.de(buscarEntidade(id));
    }

    @Transactional
    public TagResponse salvar(TagRequest dados) {
        String nome = normalizar(dados.nome());
        if (tagRepository.existsByNomeIgnoreCase(nome)) {
            throw nomeDuplicado(nome);
        }
        Tag tag = new Tag();
        tag.setNome(nome);
        tag.setCor(dados.cor());
        return TagResponse.de(tagRepository.save(tag));
    }

    @Transactional
    public TagResponse atualizar(int id, TagRequest dados) {
        Tag tag = buscarEntidade(id);
        String nome = normalizar(dados.nome());
        if (tagRepository.existsByNomeIgnoreCaseAndIdNot(nome, id)) {
            throw nomeDuplicado(nome);
        }
        tag.setNome(nome);
        tag.setCor(dados.cor());
        // A mudanca aparece em todos os projetos que usam a tag: eles guardam so a referencia (id)
        return TagResponse.de(tag);
    }

    @Transactional
    public void deletar(int id) {
        Tag tag = buscarEntidade(id);
        long emUso = projetoRepository.countByTagsId(id);
        if (emUso > 0) {
            throw new ConflitoException("A tag '" + tag.getNome() + "' esta em uso por " + emUso
                    + " projeto(s); remova-a dos projetos antes de apagar");
        }
        tagRepository.delete(tag);
    }

    // --- auxiliares ---

    private Tag buscarEntidade(int id) {
        return tagRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Tag nao encontrada com o ID: " + id));
    }

    /** " Spring  Boot " -> "Spring Boot": tira espacos das pontas e espacos repetidos no meio. */
    private String normalizar(String nome) {
        return nome.trim().replaceAll("\\s+", " ");
    }

    private ConflitoException nomeDuplicado(String nome) {
        return new ConflitoException("Ja existe uma tag com o nome '" + nome + "'");
    }
}
