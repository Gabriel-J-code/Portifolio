package dev.gabriel.portfolio_api.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.gabriel.portfolio_api.entities.Tag;

public interface TagRepository extends JpaRepository<Tag, Integer> {

    List<Tag> findByProjetosId(int projetoId);

    List<Tag> findAllByOrderByNomeAsc();

    // IgnoreCase: "java" conta como duplicado de "Java", igual a restricao UNIQUE do SQL Server
    // (a collation padrao do SQL Server compara textos sem diferenciar maiusculas/minusculas)
    boolean existsByNomeIgnoreCase(String nome);

    // Versao para o PUT: ignora a propria tag (renomear "java" para "Java" e permitido)
    boolean existsByNomeIgnoreCaseAndIdNot(String nome, int id);
}
