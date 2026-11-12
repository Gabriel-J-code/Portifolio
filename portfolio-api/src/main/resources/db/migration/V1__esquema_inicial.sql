-- =====================================================================
-- V1: esquema inicial do portfolio.
--
-- Regras do Flyway:
--  * Este arquivo NUNCA deve ser editado depois de aplicado: o Flyway guarda o checksum
--    na tabela flyway_schema_history e recusa subir se ele mudar.
--  * Qualquer mudanca futura vira um arquivo novo: V2__descricao.sql, V3__...
--
-- Constraints com nome explicito (pk_, uk_, fk_, ck_): mensagens de erro legiveis
-- e nomes iguais em todos os ambientes (dev, testes, producao).
-- =====================================================================

-- ---------- Conteudo do site ----------

CREATE TABLE projetos (
    id               INT IDENTITY     NOT NULL,
    titulo           NVARCHAR(255)    NOT NULL,
    descricao        NVARCHAR(MAX)    NOT NULL,
    tipo             NVARCHAR(20)     NOT NULL,
    papel            NVARCHAR(MAX)    NULL,     -- projetos com codigo privado: atuacao no lugar do GitHub
    desafios         NVARCHAR(MAX)    NULL,
    capa_url         NVARCHAR(255)    NULL,
    github_url       NVARCHAR(255)    NULL,
    visualizacao_url NVARCHAR(255)    NULL,
    ano_inicio       INT              NOT NULL,
    ano_fim          INT              NULL,     -- vazio = em andamento ("2023 – atual")
    CONSTRAINT pk_projetos PRIMARY KEY (id),
    CONSTRAINT ck_projetos_tipo CHECK (tipo IN ('PROFISSIONAL', 'ACADEMICO'))
);

-- Nome unico SEM diferenciar maiusculas: depende da collation padrao do SQL Server (..._CI_AS)
CREATE TABLE tags (
    id   INT IDENTITY  NOT NULL,
    nome NVARCHAR(255) NOT NULL,
    cor  NVARCHAR(255) NOT NULL,
    CONSTRAINT pk_tags PRIMARY KEY (id),
    CONSTRAINT uk_tags_nome UNIQUE (nome)
);

-- Ligacao N:N entre projetos e tags. A PK composta impede o mesmo par duas vezes.
CREATE TABLE projeto_tag (
    id_projeto INT NOT NULL,
    id_tag     INT NOT NULL,
    CONSTRAINT pk_projeto_tag PRIMARY KEY (id_projeto, id_tag),
    CONSTRAINT fk_projeto_tag_projeto FOREIGN KEY (id_projeto) REFERENCES projetos (id),
    CONSTRAINT fk_projeto_tag_tag FOREIGN KEY (id_tag) REFERENCES tags (id)
);
-- O SQL Server nao cria indice para FK sozinho. A PK ja cobre buscas por id_projeto;
-- este cobre "projetos desta tag" (?tagId=) e a checagem de tag em uso.
CREATE INDEX ix_projeto_tag_tag ON projeto_tag (id_tag);

CREATE TABLE imagens (
    id         INT IDENTITY  NOT NULL,
    url        NVARCHAR(255) NOT NULL,
    descricao  NVARCHAR(255) NULL,
    id_projeto INT           NOT NULL,
    CONSTRAINT pk_imagens PRIMARY KEY (id),
    CONSTRAINT fk_imagens_projeto FOREIGN KEY (id_projeto) REFERENCES projetos (id)
);
CREATE INDEX ix_imagens_projeto ON imagens (id_projeto);

CREATE TABLE competencias (
    id        INT IDENTITY  NOT NULL,
    nome      NVARCHAR(255) NOT NULL,
    categoria NVARCHAR(20)  NOT NULL,
    icon_url  NVARCHAR(255) NULL,
    cor       NVARCHAR(255) NULL,
    CONSTRAINT pk_competencias PRIMARY KEY (id),
    CONSTRAINT ck_competencias_categoria CHECK (categoria IN ('LINGUAGEM', 'FERRAMENTA', 'TECNICA', 'SOFT_SKILL'))
);

CREATE TABLE contatos (
    id       INT IDENTITY  NOT NULL,
    tipo     NVARCHAR(20)  NOT NULL,
    valor    NVARCHAR(255) NOT NULL,
    icon_url NVARCHAR(255) NULL,
    ordem    INT           NOT NULL,
    CONSTRAINT pk_contatos PRIMARY KEY (id),
    CONSTRAINT uk_contatos_tipo UNIQUE (tipo),  -- no maximo um contato de cada tipo
    CONSTRAINT ck_contatos_tipo CHECK (tipo IN ('LINKEDIN', 'GITHUB', 'EMAIL', 'CURRICULO'))
);

-- Registro unico (secao "Sobre"): id fixo = 1, sem IDENTITY; o CHECK recusa uma segunda linha.
CREATE TABLE dados_gerais (
    id              INT           NOT NULL,
    texto           NVARCHAR(MAX) NOT NULL,
    disponibilidade NVARCHAR(255) NULL,
    foto_url        NVARCHAR(255) NULL,
    CONSTRAINT pk_dados_gerais PRIMARY KEY (id),
    CONSTRAINT ck_dados_gerais_registro_unico CHECK (id = 1)
);

-- ---------- Painel administrativo ----------

-- Uma unica linha (a conta do Gabriel), criada pelo AcessoInicializador a partir do .env.
-- Senha e codigo do 2FA guardados apenas como hash BCrypt.
CREATE TABLE acessos (
    id                    INT IDENTITY   NOT NULL,
    email                 NVARCHAR(255)  NOT NULL,
    senha                 NVARCHAR(255)  NOT NULL,
    two_factor_code       NVARCHAR(255)  NULL,
    two_factor_expires_at DATETIMEOFFSET NULL,   -- Instant (UTC)
    two_factor_tentativas INT            NOT NULL CONSTRAINT df_acessos_tentativas DEFAULT 0,
    CONSTRAINT pk_acessos PRIMARY KEY (id),
    CONSTRAINT uk_acessos_email UNIQUE (email)
);
