package dev.gabriel.portfolio_api.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "competencias")
public class Competencia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @Column (nullable = false)
    private String nome;
    @Enumerated(EnumType.STRING) // grava o nome ("LINGUAGEM"), nao a posicao (0, 1...)
    @Column (nullable = false, length = 20)
    private CategoriaCompetencia categoria;
    @Column(name = "icon_url")
    private String iconUrl;
    private String cor;

    public Competencia() {
    }

    public Competencia(int id, String nome, CategoriaCompetencia categoria, String iconUrl, String cor) {
        this.id = id;
        this.nome = nome;
        this.categoria = categoria;
        this.iconUrl = iconUrl;
        this.cor = cor;
    }

    // getters and setters for all fields

    public int getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public CategoriaCompetencia getCategoria() {
        return categoria;
    }

    public void setCategoria(CategoriaCompetencia categoria) {
        this.categoria = categoria;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    public String getCor() {
        return cor;
    }

    public void setCor(String cor) {
        this.cor = cor;
    }
}
