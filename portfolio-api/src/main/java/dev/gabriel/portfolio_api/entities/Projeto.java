package dev.gabriel.portfolio_api.entities;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;

@Entity
public class Projeto {
	@Id 
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;
	@Column (nullable = false)
	private String titulo;
	@Column (nullable = false)
	private String descricao;
	@Column (name = "capa_url")
	private String capaUrl;
	@Column (name = "github_url")
	private String githubUrl;
	@Column (name = "visualizacao_url")
	private String visualizacaoUrl;
	private Date ano;

	@ManyToMany 
	private List<Tag> tags = new ArrayList<Tag>();

	@OneToMany(mappedBy = "projeto")
	private List<Imagem> imagens = new ArrayList<Imagem>();

	public Projeto() {
	}

	public Projeto(int id, String titulo, String descricao, String capaUrl, String githubUrl, String visualizacaoUrl, Date ano) {
		this.id = id;
		this.titulo = titulo;
		this.descricao = descricao;
		this.capaUrl = capaUrl;
		this.githubUrl = githubUrl;
		this.visualizacaoUrl = visualizacaoUrl;
		this.ano = ano;
	}

	//getters and setters for all fields

	public int getId() {
		return id;
	}

	public String getTitulo() {
		return titulo;
	}

	public void setTitulo(String titulo) {
		this.titulo = titulo;
	}

	public String getDescricao() {
		return descricao;
	}

	public void setDescricao(String descricao) {
		this.descricao = descricao;
	}

	public String getCapaUrl() {
		return capaUrl;
	}

	public void setCapaUrl(String capaUrl) {
		this.capaUrl = capaUrl;
	}

	public String getGithubUrl() {
		return githubUrl;
	}

	public void setGithubUrl(String githubUrl) {
		this.githubUrl = githubUrl;
	}

	public String getVisualizacaoUrl() {
		return visualizacaoUrl;
	}

	public void setVisualizacaoUrl(String visualizacaoUrl) {
		this.visualizacaoUrl = visualizacaoUrl;
	}

	public Date getAno() {
		return ano;
	}

	public void setAno(Date ano) {
		this.ano = ano;
	}

	//getters and setters for tags and imagens
	public List<Tag> getTags() {
		return tags;
	}

	public void setTags(List<Tag> tags) {
		this.tags = tags;
	}

	public List<Imagem> getImagens() {
		return imagens;
	}

	public void setImagens(List<Imagem> imagens) {
		this.imagens = imagens;
	}
}