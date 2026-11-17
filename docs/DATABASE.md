# Banco de dados - MDB

PostgreSQL 16, gerenciado por **Flyway** (`backend-java/src/main/resources/db/migration`):

- `V1__init_schema.sql` - schema completo.
- `V2__seed_data.sql` - dados de desenvolvimento (áreas, tags, 2 usuários, 20 problemas).

## Diagrama conceitual

```
 users                     areas                  tags
 ─────                     ─────                  ────
 id PK                     id PK                  id PK
 name                      name (unique)           name (unique)
 email (unique)                ▲                      ▲
 password_hash                 │ N:1                   │ N:N
 role (USER|ADMIN)             │                       │
 created_at            ┌───────┴────────┐     ┌────────┴────────┐
                        │    problems     │◄────┤  problem_tags   │
        1:N             │  ───────────    │     │  ─────────────  │
  createdBy ────────────┤ id PK           │     │ problem_id FK   │
                         │ title           │     │ tag_id FK       │
                         │ statement       │     │ PK(problem,tag) │
                         │ hint            │     └─────────────────┘
                         │ solution        │
                         │ source          │
                         │ initial_difficulty
                         │ area_id FK      │
                         │ created_by FK   │
                         │ created_at      │
                         │ updated_at      │
                         └───────┬─────────┘
                                 │ 1:N                 1:N │
                    ┌────────────┘                          └───────────────┐
                    ▼                                                       ▼
         difficulty_ratings                                    user_problem_status
         ──────────────────                                    ───────────────────
         id PK                                                  id PK
         user_id FK ─────► users.id                              user_id FK ─────► users.id
         problem_id FK ──► problems.id                           problem_id FK ──► problems.id
         difficulty (1..5)                                       status (WANT_TO_DO|DONE|FAVORITE)
         created_at / updated_at                                 created_at
         UNIQUE(user_id, problem_id)                              UNIQUE(user_id, problem_id, status)
```

## Tabelas

### `users`
| Coluna | Tipo | Notas |
|---|---|---|
| id | BIGSERIAL PK | |
| name | VARCHAR(150) NOT NULL | |
| email | VARCHAR(180) UNIQUE NOT NULL | |
| password_hash | VARCHAR(255) NOT NULL | BCrypt |
| role | VARCHAR(20) NOT NULL DEFAULT 'USER' | `USER` \| `ADMIN` |
| created_at | TIMESTAMP NOT NULL | |

### `areas`
| id BIGSERIAL PK | name VARCHAR(100) UNIQUE NOT NULL |

### `tags`
| id BIGSERIAL PK | name VARCHAR(100) UNIQUE NOT NULL |

### `problems`
| Coluna | Tipo | Notas |
|---|---|---|
| id | BIGSERIAL PK | |
| title | VARCHAR(200) NOT NULL | |
| statement, hint, solution | TEXT NOT NULL | aceitam LaTeX (`$...$`) |
| source | VARCHAR(255) NULL | opcional |
| initial_difficulty | INTEGER NOT NULL, CHECK 1..5 | dificuldade cadastrada pelo autor |
| area_id | BIGINT NOT NULL REFERENCES areas(id) | |
| created_by | BIGINT NOT NULL REFERENCES users(id) | |
| created_at, updated_at | TIMESTAMP NOT NULL | |

A **dificuldade comunitária** exibida ao usuário não é uma coluna: é calculada em tempo de
consulta como `AVG(difficulty_ratings.difficulty)` para o problema, com *fallback* para
`initial_difficulty` quando ainda não há avaliações.

### `problem_tags` (N:N problems↔tags)
`problem_id`, `tag_id` - chave primária composta, `ON DELETE CASCADE`.

### `difficulty_ratings`
| Coluna | Tipo | Notas |
|---|---|---|
| id | BIGSERIAL PK | |
| user_id, problem_id | BIGINT NOT NULL, FK | |
| difficulty | INTEGER NOT NULL, CHECK 1..5 | |
| created_at, updated_at | TIMESTAMP NOT NULL | |
| **UNIQUE(user_id, problem_id)** | | garante uma avaliação por usuário/problema (editável via `PUT`) |

### `user_problem_status`
| Coluna | Tipo | Notas |
|---|---|---|
| id | BIGSERIAL PK | |
| user_id, problem_id | BIGINT NOT NULL, FK | |
| status | VARCHAR(20) NOT NULL, CHECK IN (...) | `WANT_TO_DO` \| `DONE` \| `FAVORITE` |
| created_at | TIMESTAMP NOT NULL | |
| **UNIQUE(user_id, problem_id, status)** | | um usuário pode ter várias linhas por problema (ex.: favoritar **e** marcar "quero fazer" simultaneamente), mas nunca duplicar o mesmo status |

Índices auxiliares em `problems.area_id`, `problems.created_by`,
`difficulty_ratings.problem_id`, `user_problem_status.user_id` e `.problem_id`.

## Por que não usar o CSV como banco?

O CSV é usado apenas como **formato de intercâmbio** (import/export), nunca como fonte de
verdade - todos os dados residem no PostgreSQL. Isso está implementado no `backend-python`: a
importação lê o CSV, valida linha a linha e delega a persistência ao `POST /api/problems` do
backend Java (que grava no Postgres); a exportação lê do Postgres (via API Java) e apenas serializa
para CSV sob demanda.
