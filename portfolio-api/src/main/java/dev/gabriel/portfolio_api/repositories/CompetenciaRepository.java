package dev.gabriel.portfolio_api.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.gabriel.portfolio_api.entities.Competencia;

public interface CompetenciaRepository extends JpaRepository<Competencia, Integer> {
}
