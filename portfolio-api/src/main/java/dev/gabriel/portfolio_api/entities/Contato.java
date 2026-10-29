package dev.gabriel.portfolio_api.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity 
public class Contato {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @Column(nullable = false)
    private String tipo;
    @Column(nullable = false)
    private String valor; //url ou texto
    @Column(name = "icon_url")
    private String iconUrl;

    public Contato() {
    }

    public Contato(int id, String tipo, String valor, String iconUrl) {
        this.id = id;
        this.tipo = tipo;
        this.valor = valor;
        this.iconUrl = iconUrl;
    }

    // getters and setters for all fields
    public int getId() {
        return id;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getValor() {
        return valor;
    }

    public void setValor(String valor) {
        this.valor = valor;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }
}
