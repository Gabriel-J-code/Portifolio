package dev.gabriel.portfolio_api.repositories;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.gabriel.portfolio_api.entities.Imagem;

public interface ImagemRepository extends JpaRepository<Imagem, Integer> {

    List<Imagem> findByProjetoId(int projetoId);
    
}
