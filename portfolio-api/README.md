# Portfolio API

API REST que alimenta o portfólio pessoal de **Gabriel de Jesus**: projetos (P&D/estágios e acadêmicos), competências, contatos e dados de apresentação.

O site público (Flutter Web) só **lê** os dados. O cadastro é feito por um painel administrativo privado, que usa os endpoints protegidos desta API.

> **Status:** em desenvolvimento. Projetos e Competências estão prontos e testados. Veja o [Roadmap](#roadmap).

## Stack

| Camada | Tecnologia |
|---|---|
| Linguagem / framework | Java 17, Spring Boot 4.1 (Web MVC, Data JPA, Security, Validation) |
| Banco de dados | SQL Server 2022 (via Docker) |
| Documentação | OpenAPI 3 / Swagger UI (springdoc) |
| Testes | JUnit 5, MockMvc, Testcontainers |
| Planejado | JWT + 2FA por e-mail, Redis (cache), RabbitMQ (eventos), deploy no GCP Cloud Run |

## Pré-requisitos

- **JDK 17** ou superior
- **Docker Desktop** (para o banco e para os testes)
- Não é preciso instalar o Maven: o projeto usa o Maven Wrapper (`mvnw`)

## Como rodar

### 1. Configure as variáveis de ambiente

Copie o modelo e defina as senhas:

```bash
cp .env.example .env
```

| Variável | Para quê |
|---|---|
| `DB_PASSWORD` | Senha do usuário `sa` do SQL Server. Exige 8+ caracteres com maiúscula, minúscula, número e símbolo. Evite `$`. |
| `ADMIN_PASSWORD` | Senha do usuário `admin`, usada nos endpoints protegidos (temporário, veja [Autenticação](#autenticação)). |

O `.env` está no `.gitignore` e **nunca** deve ser commitado. A aplicação o lê automaticamente (`spring.config.import`). Em produção, as mesmas chaves vêm de variáveis de ambiente reais.

### 2. Suba o banco de dados

```bash
docker compose up -d
```

Esse comando sobe o SQL Server na porta `1433` e cria o banco `portfolio` na primeira execução. Os dados ficam num volume Docker e sobrevivem quando o container é desligado.

```bash
docker compose down      # para o banco (mantém os dados)
docker compose down -v   # para o banco e APAGA os dados
```

### 3. Inicie a API

```bash
./mvnw spring-boot:run       # Linux / macOS / Git Bash
.\mvnw spring-boot:run       # Windows (PowerShell)
```

A API sobe em `http://localhost:8080`. As tabelas são criadas e atualizadas automaticamente a partir das entidades (`ddl-auto=update`).

### 4. Explore pelo Swagger

Abra **http://localhost:8080/swagger-ui.html**.

- Os endpoints `GET` funcionam direto.
- Para `POST`, `PUT` e `DELETE`, clique em **Authorize** e informe o usuário `admin` com a `ADMIN_PASSWORD` do `.env`.

## Endpoints

| Recurso | Público (sem login) | Protegido (admin) |
|---|---|---|
| **Projetos** `/api/projetos` | `GET /` (filtros `?tipo=PROFISSIONAL\|ACADEMICO` e `?tagId=`)<br>`GET /{id}` | `POST /`, `PUT /{id}`, `DELETE /{id}` |
| **Competências** `/api/competencias` | `GET /` (filtro `?categoria=LINGUAGEM\|FERRAMENTA\|TECNICA\|SOFT_SKILL`)<br>`GET /{id}` | `POST /`, `PUT /{id}`, `DELETE /{id}` |

A documentação completa de cada endpoint, com os modelos de requisição e resposta, fica no Swagger.

### Exemplo: cadastrar um projeto

```bash
curl -u admin:SUA_ADMIN_PASSWORD -X POST http://localhost:8080/api/projetos \
  -H "Content-Type: application/json" \
  -d '{
        "titulo": "BeesCaatinga",
        "descricao": "Gestão de apiários da Caatinga",
        "tipo": "PROFISSIONAL",
        "anoInicio": 2023,
        "anoFim": 2024,
        "tagIds": [1, 2],
        "imagens": [{ "url": "https://exemplo.com/tela.png", "descricao": "Tela inicial" }]
      }'
```

Resposta: `201 Created`, com o cabeçalho `Location` apontando para o novo projeto e o JSON completo, incluindo o campo calculado `"periodo": "2023 – 2024"`.

### Formato de erros

Os erros seguem o padrão **ProblemDetail** ([RFC 9457](https://www.rfc-editor.org/rfc/rfc9457)). Erros de validação trazem a lista de campos inválidos:

```json
{
  "status": 400,
  "title": "Bad Request",
  "detail": "Um ou mais campos estao invalidos",
  "instance": "/api/competencias",
  "erros": {
    "nome": "não deve estar em branco",
    "categoria": "valor 'FRAMEWORK' invalido; aceitos: [LINGUAGEM, FERRAMENTA, TECNICA, SOFT_SKILL]"
  }
}
```

| Status | Quando |
|---|---|
| `400` | JSON mal formatado, campo inválido ou regra de negócio violada (ex.: `anoFim` antes de `anoInicio`) |
| `401` | Endpoint protegido sem credenciais ou com credenciais erradas |
| `404` | Recurso (ou tag referenciada) não encontrado |

## Autenticação

Leitura (`GET`) é pública. Escrita (`POST`/`PUT`/`DELETE`) exige login.

**Hoje:** HTTP Basic com um único usuário `admin`, cuja senha vem de `ADMIN_PASSWORD`. É uma solução **temporária**, para permitir testar o CRUD.

**Planejado:** login em duas etapas com JWT:
1. `POST /api/auth/login` (e-mail + senha) → a API envia um código de 6 dígitos por e-mail.
2. `POST /api/auth/verify-2fa` (código) → a API devolve um token JWT.
3. O painel administrativo usa o token nas requisições de escrita.

## Como testar

```bash
./mvnw test       # Linux / macOS / Git Bash
.\mvnw test       # Windows (PowerShell)
```

> **O Docker Desktop precisa estar aberto.** Os testes não usam o banco de desenvolvimento: o **Testcontainers** sobe um SQL Server descartável só para eles e o remove no final. Por isso não é preciso ter `.env`, banco ou API rodando, e os seus dados locais não são afetados.

Os testes são de **integração**. Cada um simula uma requisição HTTP com o **MockMvc**, que passa pela segurança, pela validação, pelo controller, pelo service e pelo banco real, e confere status, cabeçalhos e JSON da resposta. Eles cobrem:

- **Segurança:** `GET` público; escrita sem login ou com senha errada → `401`
- **CRUD completo:** criar (`201` + `Location`), buscar, listar com filtros e ordenação, atualizar, remover (`204`)
- **Validação:** campos obrigatórios, URLs, cor `#RRGGBB`, enums inválidos e JSON quebrado → `400` com a lista de erros
- **Regras de negócio:** período inválido → `400`; tag inexistente → `404`, sem salvar nada

O banco é limpo antes de cada teste, então eles são independentes entre si. A primeira classe demora mais (~1 min) porque sobe o container. As demais reaproveitam o mesmo contexto e levam poucos segundos.

Para rodar só uma classe:

```bash
./mvnw test -Dtest=ProjetoControllerTest
```

## Estrutura do projeto

```
src/main/java/dev/gabriel/portfolio_api/
├── config/         # Segurança (SecurityConfig) e Swagger (OpenApiConfig)
├── controllers/    # Endpoints REST: traduzem HTTP <-> chamadas ao service
├── dto/            # Contratos de entrada (Request, com validação) e saída (Response)
├── entities/       # Mapeamento JPA das tabelas
├── exceptions/     # Exceções do domínio + GlobalExceptionHandler (ProblemDetail)
├── repositories/   # Interfaces Spring Data JPA
└── services/       # Regras de negócio e transações

src/test/java/dev/gabriel/portfolio_api/
├── IntegracaoTest.java        # Base dos testes: contexto + MockMvc + container
├── TestcontainersConfig.java  # SQL Server descartável para os testes
└── controllers/               # Um teste de integração por recurso
```

**Fluxo de uma requisição:** `Controller` (HTTP + `@Valid`) → `Service` (regras + `@Transactional`) → `Repository` (JPA) → SQL Server. As entidades nunca saem do service: a API recebe e devolve DTOs.

## Roadmap

- [x] Projetos: CRUD, tags, imagens, filtros
- [x] Competências: CRUD por categoria
- [x] Swagger, tratamento de erros padronizado, testes de integração
- [ ] Tags, Contatos e Dados Gerais
- [ ] Login com 2FA por e-mail + JWT (substituindo o HTTP Basic)
- [ ] CORS para o domínio do front-end
- [ ] Cache com Redis nas consultas públicas
- [ ] Eventos assíncronos com RabbitMQ
- [ ] Migrações de banco (Flyway) no lugar do `ddl-auto`
- [ ] Dockerfile da API e deploy no GCP Cloud Run
