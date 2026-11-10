package dev.gabriel.portfolio_api.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.gabriel.portfolio_api.entities.Contato;
import dev.gabriel.portfolio_api.entities.TipoContato;

public interface ContatoRepository extends JpaRepository<Contato, Integer> {

    // Ordem definida no painel; em caso de empate, o cadastrado primeiro vem antes
    List<Contato> findAllByOrderByOrdemAscIdAsc();

    // Ja existe um contato deste tipo? (gera um SELECT leve, sem carregar a entidade)
    boolean existsByTipo(TipoContato tipo);

    // Mesma pergunta, ignorando o proprio registro: usado no PUT,
    // para que atualizar o LinkedIn sem trocar o tipo nao conte como duplicado
    boolean existsByTipoAndIdNot(TipoContato tipo, int id);
}
