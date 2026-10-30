package dev.gabriel.portfolio_api.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.gabriel.portfolio_api.entities.Acesso;

public interface AcessoRepository extends JpaRepository<Acesso, Integer> {

    Optional<Acesso> findByEmail(String email);
}
