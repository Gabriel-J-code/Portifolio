package dev.gabriel.portfolio_api.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.gabriel.portfolio_api.entities.Contato;

public interface ContatoRepository extends JpaRepository<Contato, Integer> {
}
