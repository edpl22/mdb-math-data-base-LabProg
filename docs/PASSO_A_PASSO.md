# Passo a passo: como o MDB funciona por dentro

Este guia explica o código das três partes do projeto (API Java, API Python e front-end) na
ordem em que uma requisição passa por elas. Para **rodar** o projeto, veja o
[README](../README.md). Aqui o foco é **entender** o que cada arquivo faz.

## Sumário

1. [Visão geral em 1 minuto](#1-visão-geral-em-1-minuto)
2. [Acompanhando uma requisição de ponta a ponta](#2-acompanhando-uma-requisição-de-ponta-a-ponta)
3. [API Java (Spring Boot), passo a passo](#3-api-java-spring-boot-passo-a-passo)
4. [API Python (Flask), passo a passo](#4-api-python-flask-passo-a-passo)
5. [Front-end (HTML/CSS/JS), passo a passo](#5-front-end-htmlcssjs-passo-a-passo)
6. [Receita: como adicionar um endpoint novo](#6-receita-como-adicionar-um-endpoint-novo)
7. [Glossário rápido](#7-glossário-rápido)

---

## 1. Visão geral em 1 minuto

```
Navegador (front-end, :8081)
   │
   ├── fetch("http://localhost:8080/api/...")   ──►  API Java (Spring Boot)  ──►  Banco (H2 ou PostgreSQL)
   │                                                     ▲
   └── fetch("http://localhost:8000/tools/...") ──►  API Python (Flask) ──────┘  (chama a API Java via HTTP)
```

| Parte | Pasta | Porta | Responsabilidade |
|---|---|---|---|
| API Java | `backend-java/` | 8080 | **Dona dos dados**: usuários, login (JWT), problemas, áreas, tags, avaliações, favoritos |
| API Python | `backend-python/` | 8000 | **Ferramentas**: sorteio, simulado, estatísticas, CSV |
| Front-end | `frontend-web/` | 8081 | Telas que o usuário vê; chama as duas APIs com `fetch` |

Regra de ouro da arquitetura: **só a API Java acessa o banco**. Quando a API Python precisa de
problemas, ela os pede à API Java por HTTP, como qualquer outro cliente faria. Assim as duas
APIs são independentes: cada uma roda no seu processo, na sua porta, e pode ser reiniciada sem
derrubar a outra.

Todas as respostas são **JSON**. Os erros também: a API Java responde `{ "message": "..." }` e a
API Python responde `{ "detail": "..." }`.

---

## 2. Acompanhando uma requisição de ponta a ponta

Exemplo: o usuário clica em **"Gerar simulado"** pedindo 5 questões de Álgebra.

**Passo 1: Front-end (`frontend-web/js/mock-exam.js`)**
O botão chama `PythonApi.mockExam(payload)`:

```js
const payload = { quantity: 5, area: 'Algebra', minDifficulty: 1, maxDifficulty: 5, includeStatements: true };
const response = await PythonApi.mockExam(payload);
```

**Passo 2: Front-end (`frontend-web/js/api.js`)**
`PythonApi.mockExam` usa a função central `request()`, que faz o `fetch`:

```js
mockExam: (payload) => request(CONFIG.PYTHON_API_BASE, '/tools/mock-exam', { method: 'POST', body: payload }),
```

`request()` monta o header `Content-Type: application/json`, transforma o objeto em texto com
`JSON.stringify`, envia e, se vier erro, lança um `ApiError` com a mensagem do servidor.

**Passo 3: API Python (`backend-python/app/main.py`)**
O Flask encontra a função decorada com `@app.post("/tools/mock-exam")`:

```python
@app.post("/tools/mock-exam")
def mock_exam():
    body = request.get_json(silent=True)                 # lê o JSON enviado
    exam_request = MockExamRequest.model_validate(body)  # valida (quantidade 1..100 etc.)
    problems = build_mock_exam(java_client(), get_settings(), exam_request)
    ...
    return jsonify(MockExamResponse(problems=problems).model_dump())  # devolve JSON
```

**Passo 4: API Python → API Java (`app/services/` e `app/clients/java_client.py`)**
`build_mock_exam` precisa saber quais problemas existem. Ele chama
`client.search_problems(...)`, que faz um `GET http://localhost:8080/api/problems?area=Algebra...`
usando a biblioteca `httpx`. Depois sorteia 5 problemas sem repetição (`random.sample`). Como
`includeStatements` é `true`, ele também busca o enunciado de cada um com
`GET /api/problems/{id}`.

**Passo 5: API Java (`ProblemController` → `ProblemService` → `ProblemRepository`)**
O Spring recebe o `GET /api/problems`. O controller repassa os filtros ao service, que consulta o
banco pelo repository e devolve uma página de resultados em JSON.

**Passo 6: volta para o navegador**
A API Python devolve `{ "problems": [...] }`. O `mock-exam.js` recebe a lista e a função
`renderExam()` monta a prova na tela. O MathJax renderiza as fórmulas em LaTeX (`$x^2$`).

> Para ver isso acontecendo, abra a aba **Network** do navegador (F12). Aparece o
> `POST :8000/tools/mock-exam`. As chamadas que a
> Python faz para a Java são de servidor para servidor, então aparecem no terminal da API Java,
> não no navegador.

---

## 3. API Java (Spring Boot), passo a passo

### 3.1 Estrutura de pastas

```
backend-java/src/main/java/com/mdb/backend/
├── MdbBackendApplication.java   ← ponto de entrada (main)
├── controller/   ← recebe as requisições HTTP (rotas)
├── service/      ← regras de negócio
├── repository/   ← acesso ao banco (Spring Data JPA)
├── entity/       ← classes que viram tabelas (Problem, User, Area...)
├── dto/          ← formato do JSON de entrada (request) e saída (response)
├── mapper/       ← converte entity → dto
├── security/     ← JWT: gerar, ler e validar o token
├── exception/    ← exceções próprias + handler global de erros
└── config/       ← segurança (quem acessa o quê), CORS, Swagger
backend-java/src/main/resources/
├── application.yml          ← configuração (porta, banco, JWT)
├── application-local.yml    ← perfil "local": banco H2, sem precisar de PostgreSQL
└── db/migration/            ← scripts SQL do Flyway (V1 cria tabelas, V2 insere dados)
```

A ideia é **separar responsabilidades em camadas**:

```
Requisição HTTP → Controller → Service → Repository → Banco
                       ▲            │
        DTO (JSON) ◄───┴── Mapper ◄─┘ Entity
```

### 3.2 Passo 1: a aplicação sobe

`MdbBackendApplication.main()` inicia o Spring Boot. Ao subir, ele:

1. Lê `application.yml` (porta 8080, dados do banco, segredo do JWT).
2. Roda o **Flyway**, que aplica `V1__init_schema.sql` (cria as tabelas) e
   `V2__seed_data.sql` (insere áreas, tags, 20 problemas e 2 usuários) **uma única vez**.
3. Cria automaticamente os objetos (*beans*) de controllers, services e repositories e os
   conecta entre si pelo construtor (injeção de dependência).

### 3.3 Passo 2: a requisição chega ao Controller

O controller declara as rotas com anotações. Exemplo resumido de `ProblemController.java`:

```java
@RestController                       // as respostas são JSON
@RequestMapping("/api/problems")      // prefixo de todas as rotas da classe
public class ProblemController {

    @GetMapping                       // GET /api/problems?text=&area=&page=...
    public ResponseEntity<PageResponse<ProblemSummaryResponse>> search(
            @RequestParam(required = false) String text,
            @RequestParam(required = false) String area, ...) {
        return ResponseEntity.ok(PageResponse.from(problemService.search(text, area, ...)));
    }

    @GetMapping("/{id}")              // GET /api/problems/7
    public ResponseEntity<ProblemDetailResponse> findById(@PathVariable Long id, ...) { ... }

    @PostMapping                      // POST /api/problems (corpo JSON)
    public ResponseEntity<ProblemDetailResponse> create(@Valid @RequestBody ProblemRequest request, ...) { ... }
}
```

| Anotação | Para que serve |
|---|---|
| `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping` | Liga um método a um verbo HTTP + rota (equivalem ao `@app.get`/`@app.post` do Flask) |
| `@PathVariable` | Pega um pedaço da URL (`/problems/{id}`) |
| `@RequestParam` | Pega um parâmetro da query string (`?area=Algebra`) |
| `@RequestBody` | Converte o JSON do corpo em um objeto Java |
| `@Valid` | Aplica as validações do DTO antes de entrar no método |

O controller **não tem regra de negócio**: só recebe, delega ao service e devolve a resposta.

### 3.4 Passo 3: validação do JSON de entrada (DTO)

Os DTOs de entrada ficam em `dto/request/`. Exemplo (`DifficultyRatingRequest`):

```java
public record DifficultyRatingRequest(
        @NotNull(message = "A dificuldade é obrigatória")
        @Min(value = 1, message = "A dificuldade deve estar entre 1 e 5")
        @Max(value = 5, message = "A dificuldade deve estar entre 1 e 5")
        Integer difficulty
) {}
```

Se o front-end mandar `{"difficulty": 9}`, o `@Valid` barra a requisição **antes** do método
rodar e o handler de erros responde `400` com a mensagem acima.

### 3.5 Passo 4: o Service aplica as regras

Exemplo: em `DifficultyRatingService.create()`, cada usuário só pode avaliar um problema uma
vez. Se já existir uma avaliação, o service lança `DuplicateResourceException`, que vira
`409 Conflict`. O `ProblemService` busca o problema e, se o id não existir, lança
`ResourceNotFoundException`, que vira `404`.

### 3.6 Passo 5: o Repository fala com o banco

Os repositories são **interfaces**: o Spring Data JPA gera a implementação sozinho.

```java
public interface ProblemRepository extends JpaRepository<Problem, Long> {
    // findById, save, delete... já vêm prontos.
    // Consultas específicas (busca com filtros) são escritas com @Query.
}
```

As **entities** (`entity/Problem.java`, `User.java`...) são classes anotadas com `@Entity` que
representam as tabelas criadas pelo Flyway.

### 3.7 Passo 6: a resposta volta como JSON

O service converte a entity em um DTO de resposta (`dto/response/`) usando o `ProblemMapper`.
Assim nunca expomos campos internos, como o hash da senha. O Spring transforma o DTO em JSON
automaticamente.

### 3.8 Segurança: login e JWT

1. `POST /api/auth/login` → `AuthService` confere a senha com **BCrypt** e gera um **token JWT**
   (`JwtService`) contendo o id, o email e o papel (`USER` ou `ADMIN`).
2. O front-end guarda o token no `localStorage` e o envia em toda requisição protegida:
   `Authorization: Bearer <token>`.
3. Em cada requisição, o `JwtAuthenticationFilter` lê esse header, valida o token e registra quem
   é o usuário.
4. `config/SecurityConfig.java` define quem pode acessar cada rota. Por exemplo: `GET` de
   problemas é público, avaliar exige login e criar ou excluir problema exige `ADMIN`.

### 3.9 Erros: o `GlobalExceptionHandler`

`exception/GlobalExceptionHandler.java` (anotado com `@RestControllerAdvice`) captura **qualquer
exceção** lançada pelos controllers e a transforma em JSON padronizado:

```json
{ "timestamp": "...", "status": 404, "error": "Not Found",
  "message": "Problema com id 999 não encontrado.", "details": [] }
```

| Situação | Status |
|---|---|
| Dado inválido (`@Valid`), JSON malformado, id não numérico | 400 |
| Sem token / token inválido | 401 |
| Sem permissão (não é ADMIN) | 403 |
| Problema ou área inexistente | 404 |
| Avaliação duplicada, área repetida | 409 |
| Qualquer outro erro | 500 (sem expor o stack trace) |

---

## 4. API Python (Flask), passo a passo

### 4.1 Estrutura de pastas

```
backend-python/
├── requirements.txt         ← dependências (flask, flask-cors, httpx, pydantic...)
├── app/
│   ├── main.py              ← cria o app Flask, declara TODAS as rotas e os handlers de erro
│   ├── clients/java_client.py   ← cliente HTTP que conversa com a API Java
│   ├── services/            ← lógica: sorteio, simulado, estatísticas, CSV
│   ├── schemas/             ← modelos Pydantic: validam a entrada e definem o formato da saída
│   └── core/
│       ├── config.py        ← configurações (URL da API Java, timeout...)
│       ├── errors.py        ← ApiError: erro "esperado" com status HTTP
│       └── security.py      ← lê o token e confere na API Java se é ADMIN
└── tests/                   ← testes com pytest (a API Java é simulada)
```

### 4.2 Passo 1: criando o app

Em `app/main.py`:

```python
from flask import Flask, jsonify, request
from flask_cors import CORS

app = Flask(__name__)
CORS(app, origins=allowed_origins)   # permite que o front-end (porta 8081) chame esta API
```

O **CORS** é necessário porque o navegador bloqueia, por padrão, chamadas de uma página em
`localhost:8081` para outra origem (`localhost:8000`). O `flask-cors` adiciona os headers que
autorizam essas chamadas.

Para rodar: `flask --app app.main run --port 8000`. Também funciona `python -m app.main`, por
causa do bloco `if __name__ == "__main__":` no fim do arquivo.

### 4.3 Passo 2: declarando rotas com `@app.get` e `@app.post`

Cada rota é uma função Python com um **decorator** que diz o verbo HTTP e o caminho:

```python
@app.get("/tools/random-problem")          # GET  → buscar/consultar algo
def random_problem():
    filters = problem_filters()             # lê ?area=&tag=&minDifficulty=&maxDifficulty=
    problem = pick_random_problem(java_client(), get_settings(), **filters)
    return jsonify(problem)                 # dict Python → resposta JSON

@app.post("/tools/mock-exam")              # POST → enviar dados para processar
def mock_exam():
    body = request.get_json(silent=True)    # corpo JSON enviado pelo front-end
    ...
```

Todas as rotas da API Python:

| Decorator | Função | O que faz |
|---|---|---|
| `@app.get("/health")` | `health()` | Diz se a API está no ar e se alcança a API Java |
| `@app.get("/tools/random-problem")` | `random_problem()` | Sorteia um problema com os filtros |
| `@app.post("/tools/mock-exam")` | `mock_exam()` | Gera um simulado sem repetição |
| `@app.get("/tools/stats")` | `catalog_stats()` | Estatísticas do catálogo |
| `@app.post("/tools/csv/import")` | `import_csv()` | Importa CSV (ADMIN) |
| `@app.get("/tools/csv/export")` | `export_csv()` | Exporta CSV (ADMIN) |

> `@app.get("/x")` é um atalho do Flask para `@app.route("/x", methods=["GET"])`. O mesmo vale
> para `@app.post`.

O que o Flask oferece dentro da função:

| Objeto | Uso |
|---|---|
| `request.args.get("area")` | Parâmetro da URL (`?area=Algebra`) |
| `request.get_json()` | Corpo JSON de um POST |
| `request.files.get("file")` | Arquivo enviado em formulário (upload do CSV) |
| `request.headers.get("Authorization")` | Header com o token |
| `jsonify(...)` | Transforma `dict`/`list` em resposta JSON |

### 4.4 Passo 3: validando a entrada (Pydantic)

`app/schemas/mock_exam.py` descreve o JSON esperado e suas regras:

```python
class MockExamRequest(BaseModel):
    quantity: int = Field(gt=0, le=100)          # 1 a 100
    area: Optional[str] = None
    minDifficulty: int = Field(default=1, ge=1, le=5)
    maxDifficulty: int = Field(default=5, ge=1, le=5)
    includeStatements: bool = False
```

`MockExamRequest.model_validate(body)` lança `ValidationError` se algo estiver errado, por
exemplo `quantity: 0`. O handler de erros transforma isso em `422` com uma mensagem legível.

### 4.5 Passo 4: buscando dados na API Java

A API Python não tem banco. `app/clients/java_client.py` faz as chamadas HTTP com `httpx`:

```python
def search_problems(self, area=None, ..., page=0, size=20):
    response = self._client.get("/api/problems", params={...})   # GET http://localhost:8080/api/problems
    self._raise_for_status(response)    # se a Java respondeu erro, vira JavaApiError
    return response.json()
```

### 4.6 Passo 5: a lógica nos services

Os services são funções Python puras, sem nada de Flask, e por isso fáceis de testar:

- `selection_service.fetch_pool()`: pagina a busca da API Java e junta todos os problemas que
  batem com os filtros.
- `random_service.pick_random_problem()`: `random.choice` no conjunto e busca o detalhe.
- `mock_exam_service.build_mock_exam()`: `random.sample` (sem repetição). Se não houver
  problemas suficientes, lança `InsufficientProblemsError`.
- `stats_service.build_stats()`: conta por área, por dificuldade e por tópico com
  `collections.Counter`.
- `csv_service`: lê o CSV com o módulo `csv`, valida linha por linha e cria cada problema com
  `POST /api/problems` na API Java.

### 4.7 Passo 6: autenticação nas rotas de CSV

A API Python **não gera tokens**: ela reaproveita o login da API Java.

1. `extract_bearer_token()` lê o header `Authorization: Bearer <token>`. Se não houver, responde
   `401`.
2. `require_admin()` chama `GET /api/users/me` na API Java com esse token e confere se
   `role == "ADMIN"`. Se não for, responde `403`.

### 4.8 Passo 7: tratamento de erros

No fim de `main.py`, funções decoradas com `@app.errorhandler(...)` convertem cada tipo de erro
em JSON `{"detail": "..."}`:

| Erro | Quando acontece | Status |
|---|---|---|
| `ApiError` | Lançado pelas rotas (filtros sem resultado, arquivo inválido, sem permissão) | o status informado (400, 401, 403, 404) |
| `ValidationError` | Pydantic recusou o JSON | 422 |
| `JavaApiError` | A API Java respondeu erro | o mesmo status da Java |
| `httpx.HTTPError` | **A API Java está fora do ar** | 503 |
| `HTTPException` | Rota inexistente ou método errado | 404 / 405 |
| `Exception` | Qualquer outro erro | 500 |

Por isso a API Python **nunca cai** por causa de uma entrada ruim, nem quando a API Java está
desligada.

---

## 5. Front-end (HTML/CSS/JS), passo a passo

### 5.1 Estrutura de pastas

```
frontend-web/
├── index.html            ← página inicial
├── pages/                ← demais telas (catalog, problem, mock-exam, profile, login, register, admin)
├── css/style.css         ← todo o visual (cores, componentes, responsivo, impressão)
└── js/
    ├── config.js         ← URLs das duas APIs
    ├── api.js            ← TODA comunicação com as APIs (fetch)
    ├── ui.js             ← funções de interface reutilizáveis
    ├── nav.js            ← monta cabeçalho e rodapé
    └── home.js, catalog.js, problem.js, mock-exam.js, profile.js, auth.js, admin.js
                          ← um script por página
```

Não há framework nem etapa de build: cada página HTML carrega os scripts com `<script src>`.

### 5.2 Passo 1: a página carrega os scripts em ordem

Toda página termina assim (exemplo de `pages/catalog.html`):

```html
<script src="/js/config.js"></script>       <!-- 1. onde estão as APIs -->
<script src="/js/api.js"></script>          <!-- 2. funções que chamam as APIs -->
<script src="/js/ui.js"></script>           <!-- 3. helpers visuais -->
<script src="/js/nav.js"></script>          <!-- 4. cabeçalho e rodapé -->
<script>window.MDB_ACTIVE_PAGE = 'catalog';</script>
<script src="/js/catalog.js"></script>      <!-- 5. lógica desta página -->
```

A ordem importa: `catalog.js` usa funções definidas em `api.js` e `ui.js`.

### 5.3 Passo 2: `config.js` diz onde estão as APIs

```js
const CONFIG = Object.freeze({
    JAVA_API_BASE: 'http://localhost:8080',
    PYTHON_API_BASE: 'http://localhost:8000',
});
```

Se as APIs mudarem de porta, só este arquivo muda.

### 5.4 Passo 3: `api.js` é o único lugar que faz `fetch`

**A função `request()`** centraliza tudo o que é comum a qualquer chamada:

1. Monta os headers (`Content-Type: application/json` e, se a rota exige login,
   `Authorization: Bearer <token>`).
2. Faz o `fetch`.
3. Se a API estiver desligada, lança o erro "Não foi possível conectar à API Java/Python (porta…)".
4. Se a resposta for erro (4xx/5xx), lê `message` (Java) ou `detail` (Python) e lança `ApiError`.
5. Se der certo, devolve o JSON já convertido em objeto.

**Os objetos `JavaApi` e `PythonApi`** dão um nome legível a cada endpoint. Basta olhar o objeto
para saber qual API atende cada função:

```js
const JavaApi = {
    login: (email, password) => request(CONFIG.JAVA_API_BASE, '/api/auth/login', { method: 'POST', body: { email, password } }),
    searchProblems: (params) => request(CONFIG.JAVA_API_BASE, `/api/problems${buildQuery(params)}`, { auth: true }),
    setStatus: (id, status) => request(CONFIG.JAVA_API_BASE, `/api/problems/${id}/status`, { method: 'PUT', auth: true, body: { status } }),
    // ...
};

const PythonApi = {
    randomProblem: (params) => request(CONFIG.PYTHON_API_BASE, `/tools/random-problem${buildQuery(params)}`),
    mockExam: (payload) => request(CONFIG.PYTHON_API_BASE, '/tools/mock-exam', { method: 'POST', body: payload }),
    stats: () => request(CONFIG.PYTHON_API_BASE, '/tools/stats'),
    // ...
};
```

**O objeto `Auth`** guarda o token e o usuário no `localStorage` depois do login, para que a
sessão continue ao trocar de página.

### 5.5 Passo 4: cada página usa a API e desenha o resultado

Padrão seguido por todos os scripts de página:

```js
async function loadRecentProblems() {
    const el = document.getElementById('recent-problems');
    try {
        const page = await JavaApi.searchProblems({ size: 6 });   // 1. chama a API
        el.innerHTML = problemListHtml(page.content);             // 2. monta o HTML
        typesetMath(el);                                          // 3. renderiza o LaTeX
    } catch (err) {
        renderErrorAlert(el, err.message);                        // 4. mostra o erro
    }
}
```

| Script | Tela | Chama |
|---|---|---|
| `home.js` | Início | Python `stats`, `randomProblem` · Java `listAreas`, `searchProblems` |
| `catalog.js` | Catálogo | Java `searchProblems`, `listAreas`, `listTags` · Python `randomProblem` (com filtros) |
| `problem.js` | Problema | Java `getProblem`, `setStatus`, `rateDifficulty*` · Python `randomProblem` (mesma área) |
| `mock-exam.js` | Simulado | Java `searchProblems` (prévia) · Python `mockExam` |
| `auth.js` | Login/Cadastro | Java `login`, `register` |
| `profile.js` | Meu perfil | Java `myProblems` |
| `admin.js` | Admin | Java CRUD de problemas/áreas · Python `importCsv`, `exportCsv` |

### 5.6 Passo 5: peças compartilhadas

- **`ui.js`**: `escapeHtml()` (evita injeção de HTML com o texto vindo da API),
  `showToast()` (avisos), `renderLoading/Empty/ErrorAlert()`, `problemRowHtml()` (linha de
  problema usada em várias telas) e o seletor de dificuldade de 1 a 5.
- **`nav.js`**: desenha o cabeçalho (com menu mobile e o link "Sair" quando logado) e o rodapé.
- **`style.css`**: variáveis de cor no `:root`, componentes, layout responsivo
  (`@media (max-width: 960px)`) e estilo de impressão do simulado (`@media print`).

### 5.7 Passo 6: login de ponta a ponta no front-end

1. `auth.js` chama `JavaApi.login(email, senha)`.
2. A API Java devolve `{ token, user }`.
3. `Auth.setSession(token, user)` grava no `localStorage`.
4. Nas chamadas com `auth: true`, `request()` adiciona `Authorization: Bearer <token>`.
5. Se alguma resposta vier `401` (token expirado), `request()` limpa a sessão e avisa o usuário.

---

## 6. Receita: como adicionar um endpoint novo

Exemplo: um endpoint Python que conta quantos problemas existem em uma área.

**1. API Python: crie a rota em `app/main.py`**

```python
@app.get("/tools/count")
def count_problems():
    area = request.args.get("area")
    if not area:
        raise ApiError(400, "Informe o parâmetro area.")
    page = java_client().search_problems(area=area, size=1)
    return jsonify(area=area, total=page["totalElements"])
```

**2. Teste no navegador ou no terminal**

```bash
curl "http://localhost:8000/tools/count?area=Algebra"
# {"area": "Algebra", "total": 4}
```

**3. Front-end: exponha a função em `js/api.js`**

```js
const PythonApi = {
    // ...
    countProblems: (area) => request(CONFIG.PYTHON_API_BASE, `/tools/count${buildQuery({ area })}`),
};
```

**4. Use a função na página**

```js
const { total } = await PythonApi.countProblems('Algebra');
```

**No Java**, o caminho é o mesmo em camadas: adicione um método com `@GetMapping`/`@PostMapping`
no controller, coloque a regra no service e, se precisar de uma consulta nova, adicione-a no
repository. Se a rota tiver regra de acesso própria, inclua-a em `SecurityConfig`.

**Teste automatizado (Python)**: em `tests/test_api.py`, use `use_java(handler)` para simular a
API Java e `client.get("/tools/count?area=Algebra")` para chamar a rota.

---

## 7. Glossário rápido

| Termo | Significado |
|---|---|
| **API REST** | Serviço que expõe dados por URLs e verbos HTTP (GET buscar, POST criar, PUT alterar, DELETE remover) |
| **JSON** | Formato de texto usado para trocar dados entre front-end e APIs |
| **Endpoint / rota** | Uma combinação de verbo + caminho, ex.: `POST /tools/mock-exam` |
| **Decorator** (`@app.get`) | No Python, "embrulha" uma função e a registra como rota no Flask |
| **Anotação** (`@GetMapping`) | No Java, metadado que diz ao Spring que o método atende uma rota |
| **DTO** | Classe que define o formato do JSON de entrada ou saída (Java) |
| **Pydantic / schema** | Classe que valida o JSON de entrada e define o de saída (Python) |
| **JWT** | Token assinado que prova quem é o usuário; vai no header `Authorization` |
| **CORS** | Regra do navegador para chamadas entre origens diferentes (portas diferentes contam) |
| **Flyway / migration** | Scripts SQL versionados que criam e populam o banco automaticamente |
| **Status HTTP** | 200 OK · 201 criado · 400 dado inválido · 401 sem login · 403 sem permissão · 404 não encontrado · 409 conflito · 422 validação · 503 serviço indisponível |
