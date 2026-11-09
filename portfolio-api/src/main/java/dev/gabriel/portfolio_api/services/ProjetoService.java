package dev.gabriel.portfolio_api.services;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.gabriel.portfolio_api.entities.Imagem;
import dev.gabriel.portfolio_api.entities.Projeto;
import dev.gabriel.portfolio_api.entities.Tag;
import dev.gabriel.portfolio_api.entities.TipoProjeto;
import dev.gabriel.portfolio_api.exceptions.RecursoNaoEncontradoException;
import dev.gabriel.portfolio_api.repositories.ProjetoRepository;
import dev.gabriel.portfolio_api.repositories.TagRepository;

@Service
public class ProjetoService {

    private final ProjetoRepository projetoRepository;
    private final TagRepository tagRepository;

    // Injecao por construtor (dispensa @Autowired e facilita testes)
    public ProjetoService(ProjetoRepository projetoRepository, TagRepository tagRepository) {
        this.projetoRepository = projetoRepository;
        this.tagRepository = tagRepository;
    }

    /** Todos os projetos, ou apenas os de um carrossel quando "tipo" for informado. */
    @Transactional(readOnly = true)
    public List<Projeto> listar(TipoProjeto tipo) {
        if (tipo == null) {
            return projetoRepository.findAllByOrderByAnoInicioDesc();
        }
        return projetoRepository.findByTipoOrderByAnoInicioDesc(tipo);
    }

    @Transactional(readOnly = true)
    public Projeto buscarPorId(int id) {
        return projetoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Projeto nao encontrado com o ID: " + id));
    }

    @Transactional(readOnly = true)
    public List<Projeto> listarPorTag(int tagId) {
        return projetoRepository.findByTagsId(tagId);
    }

    @Transactional
    public Projeto salvar(Projeto projeto) {
        validarPeriodo(projeto);
        projeto.setTags(carregarTags(projeto.getTags()));
        vincularImagens(projeto, projeto.getImagens());
        return projetoRepository.save(projeto);
    }

    @Transactional
    public Projeto atualizar(int id, Projeto dados) {
        Projeto projeto = buscarPorId(id);

        projeto.setTitulo(dados.getTitulo());
        projeto.setDescricao(dados.getDescricao());
        projeto.setTipo(dados.getTipo());
        projeto.setPapel(dados.getPapel());
        projeto.setDesafios(dados.getDesafios());
        projeto.setCapaUrl(dados.getCapaUrl());
        projeto.setGithubUrl(dados.getGithubUrl());
        projeto.setVisualizacaoUrl(dados.getVisualizacaoUrl());
        projeto.setAnoInicio(dados.getAnoInicio());
        projeto.setAnoFim(dados.getAnoFim());
        validarPeriodo(projeto);

        projeto.setTags(carregarTags(dados.getTags()));

        vincularImagens(projeto, dados.getImagens());

        return projetoRepository.save(projeto);
    }

    @Transactional
    public void deletar(int id) {
        if (!projetoRepository.existsById(id)) {
            throw new RecursoNaoEncontradoException("Projeto nao encontrado com o ID: " + id);
        }
        projetoRepository.deleteById(id);
    }

    // --- auxiliares ---

    private void validarPeriodo(Projeto projeto) {
        if (projeto.getAnoInicio() == null) {
            throw new IllegalArgumentException("O ano de inicio e obrigatorio");
        }
        if (projeto.getAnoFim() != null && projeto.getAnoFim() < projeto.getAnoInicio()) {
            throw new IllegalArgumentException("O ano de fim nao pode ser anterior ao ano de inicio");
        }
    }

    /**
     * O JSON enviado traz as tags apenas com o id (ex.: "tags": [{"id": 1}, {"id": 3}]).
     * Aqui buscamos as tags reais no banco para associar ao projeto.
     */
    private List<Tag> carregarTags(List<Tag> tagsRecebidas) {
        if (tagsRecebidas == null || tagsRecebidas.isEmpty()) {
            return new ArrayList<>();
        }
        List<Integer> ids = tagsRecebidas.stream().map(Tag::getId).toList();
        List<Tag> tags = tagRepository.findAllById(ids);
        if (tags.size() != ids.size()) {
            throw new RecursoNaoEncontradoException("Uma ou mais tags informadas nao existem: " + ids);
        }
        return new ArrayList<>(tags);
    }

    /**
     * Imagem e o lado "dono" da relacao: cada uma precisa apontar para o projeto antes de salvar.
     * A lista do projeto e substituida pelas imagens recebidas; com orphanRemoval,
     * as que sairem da lista sao apagadas do banco.
     */
    private void vincularImagens(Projeto projeto, List<Imagem> imagens) {
        List<Imagem> novas = (imagens == null) ? new ArrayList<>() : new ArrayList<>(imagens);
        projeto.getImagens().clear();
        for (Imagem imagem : novas) {
            imagem.setProjeto(projeto);
            projeto.getImagens().add(imagem);
        }
    }
}
