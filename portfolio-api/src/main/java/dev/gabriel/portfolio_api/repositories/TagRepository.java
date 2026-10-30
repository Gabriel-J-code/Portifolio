package dev.gabriel.portfolio_api.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.gabriel.portfolio_api.entities.Tag;

public interface TagRepository extends JpaRepository<Tag, Integer> {
    
    List<Tag> findByProjetosId(int projetoId);
}
