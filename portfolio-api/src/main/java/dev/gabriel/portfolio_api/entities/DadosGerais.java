package dev.gabriel.portfolio_api.entities;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Tabela de registro unico (singleton): a secao "Sobre" do site.
 *
 * Por que o id e fixo e nao IDENTITY (autoincremento)?
 * - Com IDENTITY, apagar e recriar o registro geraria ID 2, 3...
 * - O SQL Server pode "pular" valores de IDENTITY apos um reinicio.
 * Com o id sempre = 1, atribuido pelo codigo, e a check constraint abaixo,
 * o proprio banco recusa uma segunda linha.
 */
@Entity
@Table(name = "dados_gerais",
        check = @CheckConstraint(name = "ck_dados_gerais_registro_unico", constraint = "id = 1"))
public class DadosGerais {

    public static final int ID_UNICO = 1;

    @Id // sem @GeneratedValue: o valor e sempre ID_UNICO (e nao ha setter)
    private int id = ID_UNICO;

    @Column(nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String texto; // texto de apresentacao ("Ola, meu nome e Gabriel...")

    private String disponibilidade; // texto do selo, ex.: "Disponivel para contratacao"; vazio = sem selo

    @Column(name = "foto_url")
    private String fotoUrl;

    // Construtores, Getters e Setters
    public DadosGerais() {
    }

    public int getId() {
        return id;
    }
    public String getTexto() {
        return texto;
    }
    public void setTexto(String texto) {
        this.texto = texto;
    }
    public String getDisponibilidade() {
        return disponibilidade;
    }
    public void setDisponibilidade(String disponibilidade) {
        this.disponibilidade = disponibilidade;
    }
    public String getFotoUrl() {
        return fotoUrl;
    }
    public void setFotoUrl(String fotoUrl) {
        this.fotoUrl = fotoUrl;
    }


}
