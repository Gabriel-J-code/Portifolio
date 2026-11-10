package dev.gabriel.portfolio_api.services;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.gabriel.portfolio_api.dto.ProjetoRequest;
import dev.gabriel.portfolio_api.dto.ProjetoResponse;
import dev.gabriel.portfolio_api.entities.Imagem;
import dev.gabriel.portfolio_api.entities.Projeto;
import dev.gabriel.portfolio_api.entities.Tag;
import dev.gabriel.portfolio_api.entities.TipoProjeto;
import dev.gabriel.portfolio_api.exceptions.RecursoNaoEncontradoException;
import dev.gabriel.portfolio_api.exceptions.RegraDeNegocioException;
import dev.gabriel.portfolio_api.repositories.ProjetoRepository;
import dev.gabriel.portfolio_api.repositories.TagRepository;

/**
 * Recebe e devolve DTOs; a entidade Projeto nao sai daqui.
 * A conversao para ProjetoResponse acontece dentro da transacao, entao tags e imagens
 * (carregadas sob demanda/LAZY) ainda podem ser lidas do banco nesse momento.
 */
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
    public List<ProjetoResponse> listar(TipoProjeto tipo) {
        List<Projeto> projetos = (tipo == null)
                ? projetoRepository.findAllByOrderByAnoInicioDesc()
                : projetoRepository.findByTipoOrderByAnoInicioDesc(tipo);
        return projetos.stream().map(ProjetoResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public ProjetoResponse buscarPorId(int id) {
        return ProjetoResponse.de(buscarEntidade(id));
    }

    @Transactional(readOnly = true)
    public List<ProjetoResponse> listarPorTag(int tagId) {
        return projetoRepository.findByTagsId(tagId).stream().map(ProjetoResponse::de).toList();
    }

    @Transactional
    public ProjetoResponse salvar(ProjetoRequest dados) {
        Projeto projeto = new Projeto();
        aplicarDados(projeto, dados);
        return ProjetoResponse.de(projetoRepository.save(projeto));
    }

    @Transactional
    public ProjetoResponse atualizar(int id, ProjetoRequest dados) {
        Projeto projeto = buscarEntidade(id);
        aplicarDados(projeto, dados);
        // Sem save(): a entidade veio do banco dentro desta transacao, entao o Hibernate
        // detecta as mudancas e gera o UPDATE sozinho ("dirty checking").
        // flush() antecipa esse envio ao banco para que as imagens novas ja tenham ID na resposta.
        projetoRepository.flush();
        return ProjetoResponse.de(projeto);
    }

    @Transactional
    public void deletar(int id) {
        projetoRepository.delete(buscarEntidade(id));
    }

    // --- auxiliares ---

    private Projeto buscarEntidade(int id) {
        return projetoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Projeto nao encontrado com o ID: " + id));
    }

    /** Copia os campos do DTO para a entidade (usado tanto no criar quanto no atualizar). */
    private void aplicarDados(Projeto projeto, ProjetoRequest dados) {
        validarPeriodo(dados.anoInicio(), dados.anoFim());

        projeto.setTitulo(dados.titulo());
        projeto.setDescricao(dados.descricao());
        projeto.setTipo(dados.tipo());
        projeto.setPapel(dados.papel());
        projeto.setDesafios(dados.desafios());
        projeto.setCapaUrl(dados.capaUrl());
        projeto.setGithubUrl(dados.githubUrl());
        projeto.setVisualizacaoUrl(dados.visualizacaoUrl());
        projeto.setAnoInicio(dados.anoInicio());
        projeto.setAnoFim(dados.anoFim());
        projeto.setTags(carregarTags(dados.tagIds()));
        substituirImagens(projeto, dados.imagens());
    }

    // Regra que envolve DOIS campos: nao da para expressar com uma anotacao simples no DTO
    private void validarPeriodo(Integer anoInicio, Integer anoFim) {
        if (anoFim != null && anoFim < anoInicio) {
            throw new RegraDeNegocioException("O ano de fim nao pode ser anterior ao ano de inicio");
        }
    }

    /** Busca as tags reais no banco a partir dos ids recebidos (ex.: "tagIds": [1, 3]). */
    private List<Tag> carregarTags(List<Integer> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return new ArrayList<>();
        }
        Set<Integer> idsUnicos = new HashSet<>(tagIds); // ignora ids repetidos
        List<Tag> tags = tagRepository.findAllById(idsUnicos);
        if (tags.size() != idsUnicos.size()) {
            throw new RecursoNaoEncontradoException("Uma ou mais tags informadas nao existem: " + idsUnicos);
        }
        return new ArrayList<>(tags);
    }

    /**
     * Imagem e o lado "dono" da relacao: cada uma precisa apontar para o projeto.
     * A lista e substituida pelas imagens recebidas; com orphanRemoval = true,
     * as que sairem da lista sao apagadas do banco.
     * Importante: usamos clear()/add() na MESMA lista, nunca setImagens(novaLista),
     * porque o Hibernate rastreia a colecao original para saber o que remover.
     */
    private void substituirImagens(Projeto projeto, List<ProjetoRequest.ImagemRequest> imagens) {
        projeto.getImagens().clear();
        if (imagens == null) {
            return;
        }
        for (ProjetoRequest.ImagemRequest dados : imagens) {
            Imagem imagem = new Imagem();
            imagem.setUrl(dados.url());
            imagem.setDescricao(dados.descricao());
            imagem.setProjeto(projeto);
            projeto.getImagens().add(imagem);
        }
    }
}
