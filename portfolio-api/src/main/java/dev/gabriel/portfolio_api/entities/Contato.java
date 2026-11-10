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
@Table(name = "contatos")
public class Contato {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, unique = true) // no maximo um contato de cada tipo
    private TipoContato tipo;
    @Column(nullable = false)
    private String valor; // URL, ou endereco de e-mail quando tipo = EMAIL
    @Column(name = "icon_url")
    private String iconUrl;
    @Column(nullable = false)
    private Integer ordem; // posicao de exibicao no site (1 = primeiro)

    public Contato() {
    }

    public Contato(int id, TipoContato tipo, String valor, String iconUrl, Integer ordem) {
        this.id = id;
        this.tipo = tipo;
        this.valor = valor;
        this.iconUrl = iconUrl;
        this.ordem = ordem;
    }

    // getters and setters for all fields
    public int getId() {
        return id;
    }

    public TipoContato getTipo() {
        return tipo;
    }

    public void setTipo(TipoContato tipo) {
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

    public Integer getOrdem() {
        return ordem;
    }

    public void setOrdem(Integer ordem) {
        this.ordem = ordem;
    }
}
