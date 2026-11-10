package dev.gabriel.portfolio_api.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.gabriel.portfolio_api.entities.CategoriaCompetencia;
import dev.gabriel.portfolio_api.entities.Competencia;

public interface CompetenciaRepository extends JpaRepository<Competencia, Integer> {

    // Um grupo da secao, em ordem alfabetica
    List<Competencia> findByCategoriaOrderByNomeAsc(CategoriaCompetencia categoria);

    // Todas, ja agrupadas por categoria e em ordem alfabetica dentro de cada grupo
    List<Competencia> findAllByOrderByCategoriaAscNomeAsc();
}
