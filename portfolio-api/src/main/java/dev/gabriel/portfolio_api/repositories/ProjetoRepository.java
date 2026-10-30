package dev.gabriel.portfolio_api.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.gabriel.portfolio_api.entities.Projeto;

public interface ProjetoRepository extends JpaRepository<Projeto, Integer> {
    List<Projeto> findByTagsId(int tagId);
}
