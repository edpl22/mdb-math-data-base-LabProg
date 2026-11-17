# Referência de API - MDB

Documentação interativa (sempre a fonte mais atualizada):
- Java: `http://localhost:8080/swagger-ui.html`
- Python (Flask): não há Swagger; as rotas estão listadas abaixo e em `backend-python/app/main.py`

Convenções: erros do Java seguem `{ timestamp, status, error, message, details[] }`; erros do
Python (Flask) seguem `{ detail }`. Datas em ISO-8601.

## API Java (`/api`) - porta 8080

### Saúde

| Método | Rota | Auth | Descrição |
|---|---|---|---|
| GET | `/api/health` | pública | `{status, service, framework, timestamp}` |

### Autenticação (`/api/auth`) - públicas

| Método | Rota | Body | Descrição |
|---|---|---|---|
| POST | `/api/auth/register` | `{name, email, password}` | Cria usuário `USER`, retorna `{token, user}` |
| POST | `/api/auth/login` | `{email, password}` | Retorna `{token, user}` |

### Problemas (`/api/problems`)

| Método | Rota | Auth | Descrição |
|---|---|---|---|
| GET | `/api/problems?text=&area=&tag=&minDifficulty=&maxDifficulty=&page=&size=` | pública | Busca paginada e combinável |
| GET | `/api/problems/{id}` | pública* | Detalhe completo (enunciado, dica, solução, dificuldade comunitária) |
| POST | `/api/problems` | **ADMIN** | Cria problema |
| PUT | `/api/problems/{id}` | **ADMIN** | Atualiza problema |
| DELETE | `/api/problems/{id}` | **ADMIN** | Remove problema |

\* Se autenticado, a resposta inclui `userDifficulty` e `userStatuses` (favorito/quero
fazer/já fiz) do usuário logado.

Corpo de `POST`/`PUT /api/problems`:
```json
{
  "title": "Quadratic equation",
  "statement": "Solve $x^2 - 5x + 6 = 0$.",
  "area": "Algebra",
  "hint": "Try factoring.",
  "solution": "x = 2 or x = 3.",
  "difficulty": 2,
  "tags": ["equations", "factoring"],
  "source": "Algebra problem set"
}
```
Validações: `title`, `statement`, `area`, `hint`, `solution` obrigatórios e não vazios;
`difficulty` inteiro entre 1 e 5; `area` precisa existir previamente (`POST /api/areas`); `tags`
inexistentes são criadas automaticamente.

### Dificuldade (`/api/problems/{id}/difficulty`)

| Método | Rota | Auth | Descrição |
|---|---|---|---|
| GET | `/api/problems/{id}/difficulty` | pública | `{problemId, communityDifficulty, ratingsCount, userDifficulty}` |
| POST | `/api/problems/{id}/difficulty` | autenticado | Cria a avaliação do usuário logado (`{difficulty: 1..5}`); `409` se já existe |
| PUT | `/api/problems/{id}/difficulty` | autenticado | Atualiza a avaliação existente; `404` se ainda não existe |

### Status (`/api/problems/{id}/status`) - favoritar / quero fazer / já fiz

| Método | Rota | Auth | Descrição |
|---|---|---|---|
| PUT | `/api/problems/{id}/status` | autenticado | `{status: "FAVORITE"\|"WANT_TO_DO"\|"DONE"}` - idempotente |
| DELETE | `/api/problems/{id}/status?status=FAVORITE` | autenticado | Remove aquele status específico |

### Áreas e tags

| Método | Rota | Auth | Descrição |
|---|---|---|---|
| GET | `/api/areas` | pública | Lista todas as áreas |
| POST | `/api/areas` | **ADMIN** | Cria área (`{name}`) |
| GET | `/api/tags` | pública | Lista todas as tags |

### Usuário logado

| Método | Rota | Auth | Descrição |
|---|---|---|---|
| GET | `/api/users/me` | autenticado | Dados do usuário logado |
| GET | `/api/users/me/problems` | autenticado | `{favorites[], wantToDo[], done[]}` |

## API Python (`/tools`) - porta 8000

| Método | Rota | Auth | Descrição |
|---|---|---|---|
| POST | `/tools/csv/import` | **ADMIN** (Bearer do Java) | Multipart `file`; retorna `{totalRows, imported, failed, errors[]}` |
| GET | `/tools/csv/export?area=&tag=&minDifficulty=&maxDifficulty=` | **ADMIN** | Retorna `text/csv` (download) |
| GET | `/tools/random-problem?area=&tag=&minDifficulty=&maxDifficulty=` | pública | Retorna um problema completo sorteado; `404` se nada casar com os filtros |
| POST | `/tools/mock-exam` | pública | `{quantity, area?, minDifficulty?, maxDifficulty?, includeStatements?}` → `{problems: [...]}`; com `includeStatements: true` cada questão traz `statement`, `hint`, `solution`; `400` se não houver problemas suficientes |
| GET | `/tools/stats` | pública | `{totalProblems, ratedProblems, totalRatings, averageDifficulty, byArea[], byDifficulty[], topTags[]}` |
| GET | `/health` | pública | `{status, service, framework, javaApi: "ok"\|"unreachable"}` |

Erros do serviço Python: validação → `422 {detail, errors[]}`; erro vindo da API Java → mesmo
status e mensagem; API Java inacessível → `503 {detail}`.

CSV esperado (cabeçalho fixo, a coluna `id` é ignorada na importação):
```
id,titulo,enunciado,area,dificuldade,dica,solucao,fonte
1,"Quadratic equation","Solve x² - 5x + 6 = 0","Algebra",2,"Try factoring the polynomial","x = 2 or x = 3","Algebra problem set"
```

## Mapa de autorização (resumo)

| Ação | Público | Autenticado | Admin |
|---|:---:|:---:|:---:|
| Consultar problemas/áreas/tags | Sim | Sim | Sim |
| Avaliar dificuldade / favoritar / status | Não | Sim | Sim |
| Cadastrar/editar/excluir problema | Não | Não | Sim |
| Cadastrar área | Não | Não | Sim |
| Importar/exportar CSV | Não | Não | Sim |
| Sortear questão / gerar simulado | Sim | Sim | Sim |
