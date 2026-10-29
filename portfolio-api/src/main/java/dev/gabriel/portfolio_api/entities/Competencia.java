package dev.gabriel.portfolio_api.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Competencia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @Column (nullable = false)
    private String nome;
    @Column (nullable = false)
    private String categoria;
    @Column(name = "icon_url")
    private String iconUrl;
    private String cor;

    public Competencia() {
    }

    public Competencia(int id, String nome, String categoria, String iconUrl, String cor) {
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

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
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
