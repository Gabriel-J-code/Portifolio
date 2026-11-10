package dev.gabriel.portfolio_api.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.gabriel.portfolio_api.entities.Contato;

public interface ContatoRepository extends JpaRepository<Contato, Integer> {

    // Ordem definida no painel; em caso de empate, o cadastrado primeiro vem antes
    List<Contato> findAllByOrderByOrdemAscIdAsc();
}
