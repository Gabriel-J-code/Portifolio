package dev.gabriel.portfolio_api.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.gabriel.portfolio_api.entities.DadosGerais;

public interface DadosGeraisRepository extends JpaRepository<DadosGerais, Integer> {
}
