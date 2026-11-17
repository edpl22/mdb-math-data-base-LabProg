# MDB - Math Data Base

Plataforma web para quem quer estudar ou treinar matemática por área: busca com filtros,
avaliação de dificuldade pela comunidade, favoritos, sorteio de questões, gerador de simulados
para imprimir (com gabarito) e importação/exportação de problemas em CSV.

Projeto final de **Laboratório de Programação II**. São duas APIs REST, uma em Java (Spring Boot)
e outra em Python (Flask), consumidas por um front-end web.

## Integrantes

| Nome | O que fez |
|---|---|
| Palos | Back-end: arquitetura, integração entre as APIs, banco de dados (migrations Flyway), Docker Compose |
| Oliveira | API Java (Spring Boot): autenticação JWT, CRUD de problemas, áreas, tags, avaliações e status |
| Mendes | API Python (Flask): sorteio, simulados, estatísticas do catálogo, importação/exportação de CSV |
| Davi Braga | Front-end (HTML/CSS/JS): páginas, consumo das duas APIs, layout responsivo |

## Como funciona

```
Front-end (HTML/CSS/JS)  :8081
   |                      |
   | /api/...             | /tools/...
   v                      v
API Java (Spring Boot)  <---  API Python (Flask)
:8080                         :8000
   |
   v
Banco (H2 local ou PostgreSQL)
```

- **API Java**: usuários, login (JWT), problemas, áreas, tags, avaliações de dificuldade e listas
  do usuário (favoritos, quero resolver, resolvidos). É a única que acessa o banco.
- **API Python**: sorteio de questão, gerador de simulados, estatísticas e CSV. Quando precisa de
  problemas, pede para a API Java via HTTP.
- **Front-end**: páginas estáticas que chamam as duas APIs com `fetch`.

A explicação do código, arquivo por arquivo, está em [docs/PASSO_A_PASSO.md](docs/PASSO_A_PASSO.md).

## Tecnologias

- **API Java** (`backend-java/`): Java 17+, Spring Boot 3.3, Spring Web, Spring Data JPA, Spring
  Security (JWT + BCrypt), Bean Validation, Flyway, Swagger (springdoc), Maven Wrapper.
- **API Python** (`backend-python/`): Python 3.11+, Flask, Flask-CORS, httpx, Pydantic.
- **Front-end** (`frontend-web/`): HTML, CSS e JavaScript puro, MathJax para fórmulas em LaTeX.
- **Banco**: H2 em arquivo (perfil `local`) ou PostgreSQL 16 (Docker).
- **Testes**: JUnit 5, Mockito e MockMvc no Java; pytest no Python.

## Estrutura de pastas

```
backend-java/     API Java (porta 8080)
backend-python/   API Python (porta 8000)
frontend-web/     front-end (porta 8081)
database/         esquema de referência e CSV de exemplo
docs/             documentação
mobile/           app Android (extra, fora dos requisitos)
docker-compose.yml
.env.example
```

## Como executar

Precisa de **JDK 17 ou mais novo** e **Python 3.11 ou mais novo**. Não precisa instalar Maven
nem PostgreSQL. Abra três terminais na pasta do projeto e suba as partes nesta ordem.

### 1. API Java (porta 8080)

```bash
cd backend-java
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

No Windows (PowerShell ou CMD):

```bash
mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

O perfil `local` usa um banco H2 salvo em `backend-java/data/`. Na primeira vez o Maven baixa as
dependências (demora alguns minutos) e o Flyway cria as tabelas com os dados de exemplo: 7 áreas,
15 tags, 20 problemas e 2 usuários. A API está pronta quando aparecer
`Started MdbBackendApplication`.

- Teste: http://localhost:8080/api/health
- Swagger: http://localhost:8080/swagger-ui.html
- Para começar com o banco zerado, apague a pasta `backend-java/data/`.

### 2. API Python (porta 8000)

```bash
cd backend-python
python -m venv .venv
.venv\Scripts\activate            # Windows
source .venv/bin/activate         # Linux/macOS
pip install -r requirements.txt
flask --app app.main run --port 8000
```

Teste: http://localhost:8000/health (o campo `javaApi` mostra `ok` quando a API Java está no ar).

### 3. Front-end (porta 8081)

```bash
cd frontend-web
python -m http.server 8081
```

Abra http://localhost:8081. O servidor precisa ser iniciado dentro de `frontend-web/`, porque as
páginas usam caminhos como `/css` e `/js`. Os endereços das APIs ficam em
`frontend-web/js/config.js`.

### Alternativa: Docker Compose (com PostgreSQL)

```bash
cp .env.example .env
docker compose up --build
```

Sobe o PostgreSQL (5432), a API Java (8080), a API Python (8000) e o front-end com nginx (8081).
As portas podem ser trocadas no `.env`.

## Endpoints da API Java (porta 8080)

Rotas marcadas como **login** ou **ADMIN** exigem o header `Authorization: Bearer <token>`, com o
token recebido no login.

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| GET | `/api/health` | público | Verifica se a API está no ar |
| POST | `/api/auth/register` | público | Cadastro `{name, email, password}`, retorna `{token, user}` |
| POST | `/api/auth/login` | público | Login `{email, password}`, retorna `{token, user}` |
| GET | `/api/problems` | público | Busca paginada. Filtros: `text`, `area`, `tag`, `minDifficulty`, `maxDifficulty`, `page`, `size` |
| GET | `/api/problems/{id}` | público | Detalhe do problema (com login, inclui a avaliação e os status do usuário) |
| POST | `/api/problems` | ADMIN | Cria problema |
| PUT | `/api/problems/{id}` | ADMIN | Atualiza problema |
| DELETE | `/api/problems/{id}` | ADMIN | Remove problema |
| GET | `/api/problems/{id}/difficulty` | público | Média da comunidade e número de avaliações |
| POST | `/api/problems/{id}/difficulty` | login | Avalia de 1 a 5 `{difficulty}` (409 se já avaliou) |
| PUT | `/api/problems/{id}/difficulty` | login | Muda a própria avaliação |
| PUT | `/api/problems/{id}/status` | login | Marca `{status}`: `FAVORITE`, `WANT_TO_DO` ou `DONE` |
| DELETE | `/api/problems/{id}/status?status=...` | login | Desmarca um status |
| GET | `/api/areas` | público | Lista as áreas |
| POST | `/api/areas` | ADMIN | Cria área `{name}` |
| GET | `/api/tags` | público | Lista os tópicos |
| GET | `/api/users/me` | login | Dados do usuário logado |
| GET | `/api/users/me/problems` | login | Favoritos, quero resolver e resolvidos |

## Endpoints da API Python (porta 8000)

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| GET | `/health` | público | Status do serviço e da conexão com a API Java |
| GET | `/tools/random-problem` | público | Sorteia um problema. Filtros: `area`, `tag`, `minDifficulty`, `maxDifficulty` (404 se nenhum) |
| POST | `/tools/mock-exam` | público | Gera simulado `{quantity, area, minDifficulty, maxDifficulty, includeStatements}` sem repetir questões (400 se não houver problemas suficientes) |
| GET | `/tools/stats` | público | Total de problemas, distribuição por área e por dificuldade, tópicos mais frequentes |
| POST | `/tools/csv/import` | ADMIN | Importa problemas de um CSV (campo `file`). Linhas com erro são listadas e as válidas são importadas |
| GET | `/tools/csv/export` | ADMIN | Exporta o catálogo em CSV (aceita os mesmos filtros do sorteio) |

Formato do CSV: `id,titulo,enunciado,area,dificuldade,dica,solucao,fonte`. Tem um exemplo em
`database/sample_import.csv`. Mais detalhes em [docs/API.md](docs/API.md).

Exemplos pelo terminal:

```bash
curl "http://localhost:8080/api/problems?area=Algebra&size=2"
curl -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d '{"email":"demo@mdb.dev","password":"demo123"}'
curl "http://localhost:8000/tools/random-problem?minDifficulty=2"
curl -X POST http://localhost:8000/tools/mock-exam -H "Content-Type: application/json" -d '{"quantity":3,"area":"Geometry"}'
```

## Qual tela usa qual API

| Tela / ação | API | Endpoint |
|---|---|---|
| Início: "O catálogo em números" | Python | `GET /tools/stats` |
| Início: áreas e problemas recentes | Java | `GET /api/areas`, `GET /api/problems` |
| Botão "Sortear" | Python | `GET /tools/random-problem` |
| Catálogo: busca e filtros | Java | `GET /api/problems`, `GET /api/areas`, `GET /api/tags` |
| Catálogo: "Sortear com estes filtros" | Python | `GET /tools/random-problem` |
| Página do problema | Java | `GET /api/problems/{id}` |
| Favoritar / quero resolver / resolvido | Java | `PUT` e `DELETE /api/problems/{id}/status` |
| Avaliar dificuldade | Java | `POST` e `PUT /api/problems/{id}/difficulty` |
| Problema: "Sortear outro" | Python | `GET /tools/random-problem?area=...` |
| Simulado: problemas disponíveis | Java | `GET /api/problems` |
| Simulado: "Gerar simulado" | Python | `POST /tools/mock-exam` |
| Entrar / criar conta | Java | `POST /api/auth/login`, `POST /api/auth/register` |
| Meu perfil | Java | `GET /api/users/me/problems` |
| Admin: problemas e áreas | Java | `/api/problems`, `/api/areas` |
| Admin: importar e exportar CSV | Python | `POST /tools/csv/import`, `GET /tools/csv/export` |

Para ver as chamadas acontecendo, abra a aba **Network** do navegador (F12): as que vão para
`localhost:8080` são da API Java e as que vão para `localhost:8000` são da API Python.

## Tratamento de erros

As duas APIs respondem sempre em JSON e não caem com dados inválidos.

- **Java** responde `{timestamp, status, error, message, details}`: 400 para dados inválidos ou
  JSON mal formado, 401 sem login, 403 sem permissão, 404 quando o problema não existe, 409 para
  avaliação duplicada e 500 para erro inesperado.
- **Python** responde `{detail}`: 400, 404 e 422 para entradas inválidas, repassa os erros da API
  Java com o mesmo status e responde 503 se a API Java estiver fora do ar.
- O front-end mostra essas mensagens na tela. Se uma das APIs estiver desligada, o aviso diz
  qual é e em que porta.

## Testes

```bash
cd backend-java && ./mvnw test      # 35 testes
cd backend-python && pytest -q      # 27 testes
```

## Usuários de teste

| Papel | Email | Senha |
|---|---|---|
| Administrador | `admin@mdb.dev` | `admin123` |
| Usuário comum | `demo@mdb.dev` | `demo123` |

Na tela de login há botões que preenchem esses dados. Essas contas e o `JWT_SECRET` padrão são
só para desenvolvimento.
