# Portfolio API

API REST que alimenta o portfólio pessoal de **Gabriel de Jesus**: projetos (P&D/estágios e acadêmicos), competências, contatos e dados de apresentação.

O site público (Flutter Web) só **lê** os dados. O cadastro é feito por um painel administrativo privado, que usa os endpoints protegidos desta API.

> **Status:** em desenvolvimento. Todo o conteúdo do site (Projetos, Tags, Competências, Contatos e Dados Gerais) e o login em duas etapas (senha + código por e-mail → JWT) estão prontos e testados. Veja o [Roadmap](#roadmap).

## Stack

| Camada | Tecnologia |
|---|---|
| Linguagem / framework | Java 17, Spring Boot 4.1 (Web MVC, Data JPA, Security, Validation, Mail) |
| Autenticação | Senha (BCrypt) + código por e-mail → JWT (OAuth2 Resource Server) |
| Banco de dados | SQL Server 2022 (via Docker) |
| Documentação | OpenAPI 3 / Swagger UI (springdoc) |
| Testes | JUnit 5, MockMvc, Testcontainers |
| Planejado | Redis (cache), RabbitMQ (eventos), deploy no GCP Cloud Run |

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
| `CORS_ORIGINS` | *(Opcional)* Sites que podem chamar a API pelo navegador, separados por vírgula (ex.: `https://seu-usuario.github.io`). Sem ela, vale o padrão de desenvolvimento: Flutter Web em `localhost`, qualquer porta. Veja [CORS](#cors). |

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

A API sobe em `http://localhost:8080`. Na inicialização, o **Flyway** cria ou atualiza as tabelas aplicando as migrações pendentes (veja [Banco de dados e migrações](#banco-de-dados-e-migrações)).

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

O `periodo` é montado pela API para o front exibir sem lógica própria, só com o ano (sem mês, para manter os cards padronizados):

| `anoInicio` | `anoFim` | `periodo` |
|---|---|---|
| 2023 | *(vazio)* | `2023 – atual` (em andamento) |
| 2024 | 2024 | `2024` |
| 2022 | 2024 | `2022 – 2024` |

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

Limitações conhecidas: não há **refresh token** (depois de 2 horas, faz-se login de novo) nem **revogação** de token. Um token vazado vale até expirar; em caso de emergência, trocar o `JWT_SECRET` invalida todos os tokens de uma vez. Também não há limite de tentativas de **senha** na etapa 1 (*rate limiting*): com o 2FA, descobrir a senha não basta para entrar, e o BCrypt já torna cada tentativa lenta.

### CORS

O front-end (Flutter Web) fica em outro domínio, e o navegador só entrega a ele as respostas da API se ela autorizar aquela **origem**. As origens permitidas vêm de `CORS_ORIGINS` (padrão: `http://localhost:[*]` e `http://127.0.0.1:[*]`, em que `[*]` significa qualquer porta, porque o Flutter usa uma porta aleatória em desenvolvimento). Em produção, use só o domínio real do front.

- Métodos `GET`, `POST`, `PUT` e `DELETE`; cabeçalhos `Authorization` e `Content-Type`; o `Location` do `201` fica visível para o front.
- Sem cookies (`allowCredentials=false`): o token vai no cabeçalho.
- Origem não permitida → `403`.
- O CORS **não** é proteção da API: `curl` e Postman o ignoram. Ele protege o **usuário**, impedindo que um site qualquer use o navegador dele para chamar a API. Quem protege a escrita continua sendo o token.

## Como testar

```bash
./mvnw test       # Linux / macOS / Git Bash
.\mvnw test       # Windows (PowerShell)
```

> **O Docker Desktop precisa estar aberto.** Os testes não usam o banco de desenvolvimento: o **Testcontainers** sobe um SQL Server descartável só para eles e o remove no final. Por isso não é preciso ter `.env`, banco ou API rodando, e os seus dados locais não são afetados.

Os testes são de **integração**. Cada um simula uma requisição HTTP com o **MockMvc**, que passa pela segurança, pela validação, pelo controller, pelo service e pelo banco real, e confere status, cabeçalhos e JSON da resposta. Eles cobrem:

- **Segurança:** `GET` público; escrita sem token ou com token inválido, expirado ou adulterado → `401`; login em duas etapas; CORS (preflight e origens permitidas/negadas)
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
├── config/         # Segurança (SecurityConfig, JWT, CORS), propriedades tipadas e Swagger
├── controllers/    # Endpoints REST: traduzem HTTP <-> chamadas ao service
├── dto/            # Contratos de entrada (Request, com validação) e saída (Response)
├── entities/       # Mapeamento JPA das tabelas
├── exceptions/     # Exceções do domínio + GlobalExceptionHandler (ProblemDetail)
├── repositories/   # Interfaces Spring Data JPA
├── services/       # Regras de negócio e transações
└── validacao/      # Anotações de validação próprias (regras entre campos)

src/main/resources/
├── application.properties
└── db/migration/   # Migrações do Flyway (V1__esquema_inicial.sql, ...)

src/test/java/dev/gabriel/portfolio_api/
├── IntegracaoTest.java        # Base dos testes: contexto + MockMvc + container
├── TestcontainersConfig.java  # SQL Server descartável para os testes
├── controllers/               # Um teste de integração por recurso (+ segurança JWT e CORS)
├── entities/                  # Testes unitários de regras das entidades
└── services/                  # Testes unitários de services
```

**Fluxo de uma requisição:** `Controller` (HTTP + `@Valid`) → `Service` (regras + `@Transactional`) → `Repository` (JPA) → SQL Server. As entidades nunca saem do service: a API recebe e devolve DTOs.

## Banco de dados e migrações

O esquema do banco é definido por scripts SQL versionados em `src/main/resources/db/migration`, aplicados pelo **Flyway** ao subir a API. O Flyway registra na tabela `flyway_schema_history` quais versões já rodaram e aplica só as novas, então todo ambiente (desenvolvimento, testes e produção) chega exatamente ao mesmo esquema.

O Hibernate **não cria nem altera tabelas** (`ddl-auto=validate`): ele só confere, na inicialização, se as entidades batem com o banco. Se faltar uma coluna, a API não sobe, e o erro aparece logo, não no meio de uma requisição.

**Para mudar o banco** (nova coluna, tabela, índice...):
1. Crie um arquivo novo com o próximo número, por exemplo `V2__adiciona_coluna_x.sql` (dois sublinhados depois da versão).
2. Altere a entidade correspondente.
3. Rode os testes: o Testcontainers aplica todas as migrações num banco vazio e o Hibernate valida o resultado.

> **Nunca edite uma migração que já foi aplicada.** O Flyway guarda o checksum de cada arquivo e recusa subir se ele mudar. Correções viram uma migração nova.

Para recomeçar o banco de desenvolvimento do zero: `docker compose down -v` e `docker compose up -d`. Na próxima inicialização, o Flyway recria tudo e o seed recria a conta do painel.

## Produção

Em produção, a API roda com o perfil **`prod`** (`SPRING_PROFILES_ACTIVE=prod`), definido em `application-prod.properties`. Ele muda o seguinte:

- **Swagger desligado:** `/swagger-ui.html` e `/v3/api-docs` respondem `404`, e o mapa das rotas do painel não fica público.
- **Sem valores padrão de desenvolvimento:** nada de `localhost`, Mailpit ou `trustServerCertificate`. Se faltar uma variável obrigatória, a API **não sobe**. A exceção é o SMTP: sem ele, a API sobe, mas o login responde `503`.
- **E-mail real:** SMTP com login e STARTTLS (porta 587), com timeouts para o login não travar se o servidor de e-mail não responder.
- **Atrás de proxy HTTPS** (`server.forward-headers-strategy=framework`): o Cloud Run recebe em HTTPS e repassa para a API em HTTP. Com isso, a API lê os cabeçalhos `X-Forwarded-*` e gera links corretos, como o `Location` do `201` com `https://` e o domínio público.
- **Sem SQL no log.**

Variáveis de ambiente do perfil `prod`, além de `ADMIN_EMAIL`, `ADMIN_PASSWORD` e `JWT_SECRET`:

| Variável | Exemplo |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `DB_URL` | `jdbc:sqlserver://<host>:1433;databaseName=portfolio;encrypt=true` |
| `DB_USER` / `DB_PASSWORD` | Usuário e senha do banco de produção |
| `MAIL_HOST` / `MAIL_PORT` | `smtp.gmail.com` / `587` (porta opcional; padrão `587`) |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | Conta SMTP (no Gmail, uma **senha de app**, não a senha da conta) |
| `MAIL_FROM` | Remetente; em geral, o próprio e-mail da conta SMTP |
| `CORS_ORIGINS` | Domínio do front, ex.: `https://seu-usuario.github.io` |
| `PORT` | Definida automaticamente pelo Cloud Run (padrão `8080`) |

Em produção, os segredos não ficam em arquivo `.env`. Eles vêm das variáveis de ambiente do serviço, de preferência por um gerenciador de segredos, como o Secret Manager do GCP.

### Imagem Docker

O `Dockerfile` gera a imagem que vai para produção. Ele tem dois estágios:

1. **build:** JDK 17 + Maven Wrapper compilam o projeto e geram o `.jar`. O `pom.xml` é copiado antes do código, então o download das dependências fica em cache e só é refeito quando o `pom.xml` muda.
2. **runtime:** só o JRE 17 e a aplicação. O `.jar` é extraído em **camadas** (dependências, loader do Spring e código); ao mudar só o código, apenas a camada da aplicação (poucos KB) muda.

A imagem já vem com `SPRING_PROFILES_ACTIVE=prod`, roda com um usuário **sem privilégios de root** e usa `-XX:MaxRAMPercentage=75` (por padrão a JVM usaria só 25% da memória do container). O `.dockerignore` impede que o `.env` entre na imagem.

> Os **testes não rodam no `docker build`**: eles precisam do Docker (Testcontainers), que não existe dentro do build. Rode `.\mvnw test` antes de gerar a imagem (no futuro, o GitHub Actions faz isso).

**Testar a imagem localmente**, com o banco e o Mailpit do `docker compose up -d`. Dentro do container, `localhost` é o próprio container; o computador é `host.docker.internal`:

```powershell
docker build -t portfolio-api .

docker run --rm -p 8080:8080 --env-file .env `
  -e DB_URL="jdbc:sqlserver://host.docker.internal:1433;databaseName=portfolio;encrypt=true;trustServerCertificate=true" `
  -e DB_USER=sa `
  -e MAIL_HOST=host.docker.internal -e MAIL_PORT=1025 -e MAIL_USERNAME=teste -e MAIL_PASSWORD=teste `
  -e MAIL_FROM=teste@exemplo.com -e CORS_ORIGINS=https://exemplo.github.io `
  portfolio-api
```

Com o perfil `prod`, o Swagger responde `404` e o login responde `503`, porque o Mailpit não tem STARTTLS. Para testar o login pela imagem, use o perfil de desenvolvimento: `-e SPRING_PROFILES_ACTIVE=default` (aí só `DB_URL` e `MAIL_HOST` precisam ser passados).

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
| **Migrações com Flyway; Hibernate só valida** | O `ddl-auto=update` não altera colunas existentes e deixou o banco de desenvolvimento diferente do código (colunas obrigatórias aceitando `NULL`, `projeto_tag` sem chave primária). Com scripts versionados, o esquema é revisável no Git e idêntico em todos os ambientes. |
| **Testes contra um SQL Server real (Testcontainers), não H2** | Um banco em memória diferente esconderia comportamentos do SQL Server, como `NVARCHAR(MAX)`, comparação sem diferenciar maiúsculas e as constraints. |

## Roadmap

- [x] Projetos: CRUD, tags, imagens, filtros
- [x] Competências: CRUD por categoria
- [x] Contatos: CRUD com ordem de exibição e validação do valor por tipo
- [x] Tags: CRUD com nome único e proteção contra apagar tag em uso
- [x] Dados Gerais: registro único (GET + PUT)
- [x] Swagger, tratamento de erros padronizado, testes de integração
- [x] Login em duas etapas: senha + código por e-mail (Mailpit em desenvolvimento) → token JWT
- [ ] Upload de imagens pelo painel (Google Cloud Storage), quando o painel existir
- [x] CORS para o front-end (origens configuráveis por ambiente)
- [ ] Cache com Redis nas consultas públicas
- [ ] Eventos assíncronos com RabbitMQ
- [x] Migrações de banco (Flyway) no lugar do `ddl-auto`
- [x] Perfil de produção (Swagger desligado, SMTP real, proxy HTTPS, sem padrões de desenvolvimento)
- [x] Dockerfile da API (multi-stage, camadas, usuário sem root)
- [ ] Deploy no GCP Cloud Run
