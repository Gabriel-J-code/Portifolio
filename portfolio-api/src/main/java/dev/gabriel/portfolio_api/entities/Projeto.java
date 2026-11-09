package dev.gabriel.portfolio_api.entities;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "projetos")
public class Projeto {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@Column(nullable = false)
	private String titulo;

	@Column(nullable = false, columnDefinition = "NVARCHAR(MAX)")
	private String descricao;

	// Carrossel em que o projeto aparece (PROFISSIONAL ou ACADEMICO)
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private TipoProjeto tipo;

	// Para projetos com codigo privado (empresa): descreve sua atuacao no lugar do link do GitHub
	@Column(columnDefinition = "NVARCHAR(MAX)")
	private String papel;

	@Column(columnDefinition = "NVARCHAR(MAX)")
	private String desafios;

	@Column(name = "capa_url")
	private String capaUrl;

	@Column(name = "github_url") // opcional: vazio quando o codigo e privado
	private String githubUrl;

	@Column(name = "visualizacao_url")
	private String visualizacaoUrl;

	@Column(name = "ano_inicio", nullable = false)
	private Integer anoInicio;

	@Column(name = "ano_fim") // opcional: vazio ou igual ao inicio = exibe apenas um ano
	private Integer anoFim;

	@ManyToMany
	@JoinTable(
		name = "projeto_tag",
		joinColumns = @JoinColumn(name = "id_projeto"),
		inverseJoinColumns = @JoinColumn(name = "id_tag"))
	private List<Tag> tags = new ArrayList<Tag>();

	@OneToMany(mappedBy = "projeto", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Imagem> imagens = new ArrayList<Imagem>();

	public Projeto() {
	}

	public Projeto(String titulo, String descricao, TipoProjeto tipo, Integer anoInicio, Integer anoFim) {
		this.titulo = titulo;
		this.descricao = descricao;
		this.tipo = tipo;
		this.anoInicio = anoInicio;
		this.anoFim = anoFim;
	}

	/**
	 * Texto pronto para o front exibir. Nao vira coluna no banco
	 * (o JPA so mapeia atributos), mas o JSON da API inclui "periodo".
	 * Ex.: 2024 -> "2024" | 2022 a 2024 -> "2022 – 2024"
	 */
	public String getPeriodo() {
		if (anoInicio == null) {
			return null;
		}
		if (anoFim == null || anoFim.equals(anoInicio)) {
			return String.valueOf(anoInicio);
		}
		return anoInicio + " – " + anoFim;
	}

	// getters and setters

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

	public TipoProjeto getTipo() {
		return tipo;
	}

	public void setTipo(TipoProjeto tipo) {
		this.tipo = tipo;
	}

	public String getPapel() {
		return papel;
	}

	public void setPapel(String papel) {
		this.papel = papel;
	}

	public String getDesafios() {
		return desafios;
	}

	public void setDesafios(String desafios) {
		this.desafios = desafios;
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

	public Integer getAnoInicio() {
		return anoInicio;
	}

	public void setAnoInicio(Integer anoInicio) {
		this.anoInicio = anoInicio;
	}

	public Integer getAnoFim() {
		return anoFim;
	}

	public void setAnoFim(Integer anoFim) {
		this.anoFim = anoFim;
	}

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
