# Arquitetura - MDB

## Visão geral

```
                         ┌─────────────────────┐
                         │     frontend-web      │
                         │   (HTML/CSS/JS puro)   │
                         └──────────┬─────┬───────┘
                                    │     │
                     fetch /api/... │     │ fetch /tools/...
                                    ▼     ▼
                    ┌───────────────────┐   ┌─────────────────────────┐
                    │   backend-java     │   │      backend-python      │
                    │  (Spring Boot)     │◄──┤        (Flask)            │
                    │  núcleo do domínio │   │  CSV, sorteio, simulados  │
                    └─────────┬──────────┘   └───────────────────────────┘
                              │                  (consome a API Java via HTTP;
                        JDBC  │                   nunca acessa o Postgres)
                              ▼
                       ┌─────────────┐
                       │ PostgreSQL  │
                       └─────────────┘

     mobile/ (Android nativo, Java) ──── consome apenas backend-java /api/...
```

## Por que dois backends?

O enunciado da disciplina exige **um framework Python e um framework Java** trabalhando juntos.
Em vez de dividir arbitrariamente as entidades entre os dois serviços (o que forçaria os dois a
acessar as mesmas tabelas do PostgreSQL, arriscando inconsistência), a divisão foi feita por
**responsabilidade**:

- **backend-java (Spring Boot)** é o núcleo transacional: dono do schema, das regras de negócio
  centrais (unicidade de avaliação de dificuldade, permissões ADMIN/USER, autenticação) e de toda
  escrita/leitura direta no PostgreSQL via Spring Data JPA.
- **backend-python (Flask)** é uma camada de **processamento de arquivos e seleção de dados**
  (import/export de CSV, sorteio aleatório, geração de simulados) - tarefas mais naturais em
  Python (manipulação de arquivos, scripts de seleção) e que não precisam de acesso direto ao
  banco: tudo o que ele precisa (buscar problemas, cadastrar problemas a partir do CSV) já existe
  como endpoint HTTP no backend Java.

Essa divisão evita duas aplicações escrevendo diretamente nas mesmas tabelas e mantém uma única fonte de verdade para autorização: o token JWT emitido pelo login
do Java é o mesmo token repassado pelo frontend ao chamar o Python, que por sua vez o repassa de
volta ao Java sempre que precisa de uma operação autenticada (ex.: `POST /api/problems` durante a
importação de CSV, ou `GET /api/users/me` para checar se quem pediu a exportação é admin).

## Fluxos importantes

### Login

1. Frontend/mobile chama `POST /api/auth/login` no backend Java.
2. Java valida credenciais (BCrypt) e retorna um JWT (`Authorization: Bearer <token>`) + dados do
   usuário.
3. O mesmo token é usado em todas as chamadas subsequentes - inclusive para o backend Python.

### Importação de CSV (admin)

1. Frontend (painel admin) envia o arquivo para `POST /tools/csv/import` no backend Python, com o
   `Authorization: Bearer <token>` do admin logado.
2. Python chama `GET /api/users/me` no Java para confirmar que o token pertence a um `ADMIN`
   (401/403 caso contrário).
3. Python faz o parsing/validação do CSV linha a linha (campos obrigatórios, dificuldade 1–5).
4. Para cada linha válida, Python chama `POST /api/problems` no Java **com o mesmo token**, de
   forma que o Java aplica novamente suas próprias regras de negócio (ex.: área precisa existir):
   nenhuma regra é duplicada, apenas reaproveitada.
5. Linhas inválidas (na validação do Python ou rejeitadas pelo Java) são reportadas sem abortar o
   restante do lote.

### Exportação de CSV (admin)

1. `GET /tools/csv/export` no Python, com token de admin.
2. Python valida o papel do usuário (via `GET /api/users/me`), busca os problemas que casam com os
   filtros opcionais chamando `GET /api/problems` (paginado) no Java, busca o detalhe de cada um
   (`GET /api/problems/{id}`) e monta o CSV em memória, devolvendo como `text/csv`.

### Sorteio de questão / gerador de simulados

1. `GET /tools/random-problem` ou `POST /tools/mock-exam` no Python (rotas públicas, sem exigir
   login - assim como o catálogo).
2. Python busca no Java (`GET /api/problems`) o conjunto de problemas que atende aos filtros
   (área/tag/dificuldade), pagina até um limite configurável (`MDB_MAX_POOL_SIZE_FOR_SELECTION`,
   padrão 500) e aplica a seleção aleatória localmente (`random.choice` / `random.sample`).
3. Nenhuma regra de filtragem é duplicada: o Python só decide **quais** dos resultados já
   filtrados pelo Java entram na amostra aleatória.

## Autenticação e autorização

- JWT stateless (sem sessão em servidor), assinado com HMAC-SHA (`io.jsonwebtoken`/JJWT).
- Claims: `sub` (email), `userId`, `role`.
- `Spring Security` com filtro customizado (`JwtAuthenticationFilter`) popula o
  `SecurityContext` a partir do token; rotas são liberadas/restringidas por método+path em
  `SecurityConfig` (ver [docs/API.md](API.md) para o mapa completo).
- O backend Python **não valida a assinatura do JWT**: ele delega isso ao Java, sempre reenviando
  o token recebido e reagindo aos status HTTP (`401`/`403`) retornados. Isso evita manter dois
  lugares com a lógica/segredo de verificação de token.
