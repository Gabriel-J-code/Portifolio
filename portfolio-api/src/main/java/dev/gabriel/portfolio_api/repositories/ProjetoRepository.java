package dev.gabriel.portfolio_api.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.gabriel.portfolio_api.entities.Projeto;
import dev.gabriel.portfolio_api.entities.TipoProjeto;

public interface ProjetoRepository extends JpaRepository<Projeto, Integer> {
    List<Projeto> findByTagsId(int tagId);

    // Um carrossel: projetos do tipo informado, mais recentes primeiro
    List<Projeto> findByTipoOrderByAnoInicioDesc(TipoProjeto tipo);

    List<Projeto> findAllByOrderByAnoInicioDesc();

    // Quantos projetos usam a tag? (usado para impedir apagar uma tag em uso)
    long countByTagsId(int tagId);
}
