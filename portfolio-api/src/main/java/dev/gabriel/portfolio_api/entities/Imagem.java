package dev.gabriel.portfolio_api.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "imagens")
public class Imagem {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @Column (nullable = false)
    private String url;
    private String descricao;
    @JsonIgnore // evita loop Projeto -> Imagem -> Projeto na serializacao JSON
    @ManyToOne 
    @JoinColumn(name = "id_projeto", nullable = false) 
    private Projeto projeto;

    public Imagem() {
    }

    public Imagem(int id, String url, String descricao, Projeto projeto) {
        this.id = id;
        this.url = url;
        this.descricao = descricao;
        this.projeto = projeto;
    }

    //getters and setters for all fields
    public int getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public Projeto getProjeto() {
        return projeto;
    }

    public void setProjeto(Projeto projeto) {
        this.projeto = projeto;
    }
}
