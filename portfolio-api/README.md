# Portfolio API

API REST que alimenta o portfólio pessoal de **Gabriel de Jesus**: projetos (P&D/estágios e acadêmicos), competências, contatos e dados de apresentação.

O site público (Flutter Web) só **lê** os dados. O cadastro é feito por um painel administrativo privado, que usa os endpoints protegidos desta API.

> **Status:** em desenvolvimento. Todo o conteúdo do site (Projetos, Tags, Competências, Contatos e Dados Gerais) e o login em duas etapas (senha + código por e-mail → JWT) estão prontos e testados. Veja o [Roadmap](#roadmap).

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
| `ADMIN_PASSWORD` | Senha da conta do painel (veja [Autenticação](#autenticação)). |
| `JWT_SECRET` | Chave que assina os tokens de acesso: 32 bytes aleatórios em Base64. O `.env.example` traz o comando para gerar. **Quem tiver essa chave consegue forjar tokens.** |
| `ADMIN_EMAIL` | E-mail da conta do painel e destino do código de acesso. Em desenvolvimento, qualquer endereço serve: o Mailpit captura tudo. |

A conta do painel é criada automaticamente na **primeira** inicialização, se a tabela `acessos` estiver vazia. Mudar o `.env` depois **não** altera uma conta já existente.

O `.env` está no `.gitignore` e **nunca** deve ser commitado. A aplicação o lê automaticamente (`spring.config.import`). Em produção, as mesmas chaves vêm de variáveis de ambiente reais.

### 2. Suba o banco de dados e o servidor de e-mail

```bash
docker compose up -d
```

Esse comando sobe:
- o **SQL Server** na porta `1433`, criando o banco `portfolio` na primeira execução. Os dados ficam num volume Docker e sobrevivem quando o container é desligado;
- o **Mailpit**, um servidor de e-mail falso para desenvolvimento. Ele recebe os e-mails que a API envia (como o código de login) sem entregá-los a ninguém, e os mostra em **http://localhost:8025**.

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
- Para `POST`, `PUT` e `DELETE`, obtenha um token:
  1. `POST /api/auth/login` com o `ADMIN_EMAIL` e a `ADMIN_PASSWORD` do `.env`;
  2. abra **http://localhost:8025** (Mailpit) e copie o código de 6 dígitos;
  3. `POST /api/auth/verify-2fa` com o e-mail e o código → copie o `token`;
  4. clique em **Authorize** e cole o token (sem o prefixo `Bearer`). Ele vale por 2 horas.

## Endpoints

| Recurso | Público (sem login) | Protegido (admin) |
|---|---|---|
| **Projetos** `/api/projetos` | `GET /` (filtros `?tipo=PROFISSIONAL\|ACADEMICO` e `?tagId=`)<br>`GET /{id}` | `POST /`, `PUT /{id}`, `DELETE /{id}` |
| **Competências** `/api/competencias` | `GET /` (filtro `?categoria=LINGUAGEM\|FERRAMENTA\|TECNICA\|SOFT_SKILL`)<br>`GET /{id}` | `POST /`, `PUT /{id}`, `DELETE /{id}` |
| **Contatos** `/api/contatos` | `GET /` (já na ordem de exibição)<br>`GET /{id}` | `POST /`, `PUT /{id}`, `DELETE /{id}` |
| **Tags** `/api/tags` | `GET /` (ordem alfabética)<br>`GET /{id}` | `POST /`, `PUT /{id}`, `DELETE /{id}` |
| **Dados Gerais** `/api/dados-gerais` | `GET /` (`404` até a primeira configuração) | `PUT /` (cria na primeira vez, substitui nas seguintes) |
| **Autenticação** `/api/auth` | `POST /login` (etapa 1: senha → código por e-mail)<br>`POST /verify-2fa` (etapa 2: código → token) | — |

**Dados Gerais** é um registro único (a seção "Sobre": texto de apresentação, selo de disponibilidade e foto), por isso não tem `{id}`, `POST` nem `DELETE`.

As tags são as etiquetas de tecnologia dos projetos (vinculadas pelo campo `tagIds`). O nome é único **sem diferenciar maiúsculas de minúsculas** (`java` conflita com `Java`), e uma tag em uso por algum projeto não pode ser apagada (`409`): primeiro remova-a dos projetos.

Os contatos aceitam os tipos `LINKEDIN`, `GITHUB`, `EMAIL` e `CURRICULO`, com **no máximo um contato de cada tipo**. O `valor` é validado conforme o tipo: endereço de e-mail para `EMAIL` e link `http(s)` para os demais.

A documentação completa de cada endpoint, com os modelos de requisição e resposta, fica no Swagger.

### Exemplo: cadastrar um projeto

```bash
curl -X POST http://localhost:8080/api/projetos \
  -H "Authorization: Bearer SEU_TOKEN" \
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
| `409` | Conflito com dados existentes (ex.: tag com nome repetido, segundo contato do mesmo tipo, apagar tag em uso) |
| `503` | O e-mail com o código de acesso não pôde ser enviado (tente de novo) |

## Autenticação

Leitura (`GET`) é pública. Escrita (`POST`/`PUT`/`DELETE`) exige login.

O login do painel tem **duas etapas obrigatórias** (2FA por e-mail):

1. **`POST /api/auth/login`** com `{ "email", "senha" }`. Se estiverem corretos, a API gera um código de 6 dígitos, válido por 10 minutos, e o envia por e-mail. A resposta **não traz token**, só o e-mail mascarado e o prazo:
   ```json
   { "mensagem": "Codigo de acesso enviado por e-mail", "emailMascarado": "g******@exemplo.com", "expiraEm": "2026-11-10T21:05:36Z" }
   ```
   E-mail inexistente e senha errada recebem a **mesma** resposta `401`, para não revelar qual e-mail é o do painel.
2. **`POST /api/auth/verify-2fa`** com `{ "email", "codigo" }`. Se o código estiver certo e dentro do prazo, a API devolve um **token JWT**, válido por 2 horas:
   ```json
   { "token": "eyJhbGciOiJIUzI1NiJ9...", "tipo": "Bearer", "expiraEm": "2026-11-10T23:18:02Z" }
   ```
   O código é de **uso único** e aceita **até 5 erros**: no quinto, ele é invalidado e é preciso refazer a etapa 1.
3. As requisições de escrita enviam o token no cabeçalho `Authorization: Bearer <token>`. Token ausente, expirado, adulterado ou assinado com outra chave → `401`.

Medidas de segurança aplicadas:
- senha e código guardados só como hash BCrypt; código gerado com `SecureRandom`;
- só o último código enviado vale; ele é "queimado" ao ser usado e invalidado após 5 erros;
- tempo de resposta igual para e-mail certo ou errado (não revela se o e-mail existe);
- o código não fica salvo se o e-mail não puder ser enviado (`503`);
- token assinado com HS256 (chave de 256 bits), conferido a cada requisição (assinatura, expiração e emissor) pelo filtro padrão do Spring Security (OAuth2 Resource Server).

Limitações conhecidas: não há **refresh token** (depois de 2 horas, faz-se login de novo) nem **revogação** de token. Um token vazado vale até expirar; em caso de emergência, trocar o `JWT_SECRET` invalida todos os tokens de uma vez. Também não há limite de tentativas de **senha** na etapa 1 (*rate limiting*), que está no roadmap.

## Como testar

```bash
./mvnw test       # Linux / macOS / Git Bash
.\mvnw test       # Windows (PowerShell)
```

> **O Docker Desktop precisa estar aberto.** Os testes não usam o banco de desenvolvimento: o **Testcontainers** sobe um SQL Server descartável só para eles e o remove no final. Por isso não é preciso ter `.env`, banco ou API rodando, e os seus dados locais não são afetados.

Os testes são de **integração**. Cada um simula uma requisição HTTP com o **MockMvc**, que passa pela segurança, pela validação, pelo controller, pelo service e pelo banco real, e confere status, cabeçalhos e JSON da resposta. Eles cobrem:

- **Segurança:** `GET` público; escrita sem login ou com senha errada → `401`
- **CRUD completo:** criar (`201` + `Location`), buscar, listar com filtros e ordenação, atualizar, remover (`204`)
- **Validação:** campos obrigatórios, URLs, cor `#RRGGBB`, e-mail/link conforme o tipo de contato, enums inválidos e JSON quebrado → `400` com a lista de erros
- **Regras de negócio:** período inválido → `400`; tag inexistente → `404`, sem salvar nada; duplicados e tag em uso → `409`

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
├── services/       # Regras de negócio e transações
└── validacao/      # Anotações de validação próprias (regras entre campos)

src/test/java/dev/gabriel/portfolio_api/
├── IntegracaoTest.java        # Base dos testes: contexto + MockMvc + container
├── TestcontainersConfig.java  # SQL Server descartável para os testes
└── controllers/               # Um teste de integração por recurso
```

**Fluxo de uma requisição:** `Controller` (HTTP + `@Valid`) → `Service` (regras + `@Transactional`) → `Repository` (JPA) → SQL Server. As entidades nunca saem do service: a API recebe e devolve DTOs.

## Decisões de projeto

Escolhas feitas de propósito, com o motivo. O que está fora do escopo agora tem um caminho de evolução definido.

| Decisão | Por quê |
|---|---|
| **Imagens hospedadas fora da API; o banco guarda só a URL** | Um único administrador cadastra poucas imagens, raramente: um upload próprio (armazenamento, credenciais, validação de arquivos) seria complexidade sem retorno agora. As imagens são hospedadas manualmente. **Evolução:** um endpoint de upload com uma interface `StorageService` (pasta local em desenvolvimento, Google Cloud Storage em produção), sem alterar o modelo atual, que já trabalha com URLs. |
| **A API recebe e devolve DTOs, nunca entidades** | O cliente não escolhe `id` nem campos internos, a validação fica no contrato de entrada e o JSON público não muda quando o banco muda. |
| **Categorias e tipos como enums fixos** | Evitam grupos duplicados por erro de digitação (`Linguagem` vs `Linguagens`). O banco reforça com uma `CHECK` constraint e a API responde `400` listando os valores aceitos. |
| **Duplicados: validados no service e garantidos no banco** | O service dá uma mensagem clara (`409`); a restrição `UNIQUE` no banco cobre o que escapar, como duas requisições simultâneas. |
| **Dados Gerais com ID fixo + `CHECK (id = 1)`** | Com autoincremento, "sempre o ID 1" não é garantido (registro recriado ou saltos de `IDENTITY` no SQL Server). Com ID fixo, o próprio banco impede uma segunda linha. |
| **2FA sempre obrigatório** | Há uma única conta. Um caminho único de login é mais simples de testar e não deixa a opção de, por engano, proteger o painel só com a senha. |
| **JWT validado pelo OAuth2 Resource Server do Spring, não por um filtro próprio** | O filtro padrão já confere assinatura, expiração e emissor e é amplamente testado. Filtros de autenticação escritos à mão são uma fonte comum de falhas de segurança. |
| **Sem refresh token nem revogação** | Um único administrador, com token de 2 horas: refazer o login é aceitável. Trocar o `JWT_SECRET` serve como "botão de emergência". |
| **`ddl-auto=update` em desenvolvimento** | Agilidade enquanto o modelo muda. Ele não altera colunas existentes, então antes do deploy será substituído por migrações versionadas (Flyway). |
| **Testes contra um SQL Server real (Testcontainers), não H2** | Um banco em memória diferente esconderia comportamentos do SQL Server, como `NVARCHAR(MAX)`, comparação sem diferenciar maiúsculas e as constraints. |

## Roadmap

- [x] Projetos: CRUD, tags, imagens, filtros
- [x] Competências: CRUD por categoria
- [x] Contatos: CRUD com ordem de exibição e validação do valor por tipo
- [x] Tags: CRUD com nome único e proteção contra apagar tag em uso
- [x] Dados Gerais: registro único (GET + PUT)
- [x] Swagger, tratamento de erros padronizado, testes de integração
- [x] Login em duas etapas: senha + código por e-mail (Mailpit em desenvolvimento) → token JWT
- [ ] Limite de tentativas de senha no login (*rate limiting*)
- [ ] Upload de imagens pelo painel (Google Cloud Storage), quando o painel existir
- [ ] CORS para o domínio do front-end
- [ ] Cache com Redis nas consultas públicas
- [ ] Eventos assíncronos com RabbitMQ
- [ ] Migrações de banco (Flyway) no lugar do `ddl-auto`
- [ ] Dockerfile da API e deploy no GCP Cloud Run
