package dev.gabriel.portfolio_api.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.gabriel.portfolio_api.entities.Acesso;

public interface AcessoRepository extends JpaRepository<Acesso, Integer> {

    // O e-mail e salvo em minusculas e o login normaliza a entrada; IgnoreCase e uma garantia extra
    Optional<Acesso> findByEmailIgnoreCase(String email);
}
