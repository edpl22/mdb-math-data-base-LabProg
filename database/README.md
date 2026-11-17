# database/

O PostgreSQL é a única fonte de dados da aplicação. As **migrations reais** (fonte da verdade do
schema) ficam versionadas junto do backend Java, que é quem as aplica automaticamente no startup
via Flyway:

```
backend-java/src/main/resources/db/migration/
├── V1__init_schema.sql   # schema completo
└── V2__seed_data.sql     # dados de desenvolvimento (áreas, tags, usuários, 20 problemas)
```

Esta pasta contém apenas material de apoio:

- [`schema_reference.sql`](schema_reference.sql) - cópia somente-leitura do schema para consulta
  rápida sem precisar abrir o backend Java (**não é executada por nenhuma ferramenta**; edite
  sempre as migrations originais).
- [`sample_import.csv`](sample_import.csv) - arquivo de exemplo para testar a importação de CSV
  pelo painel administrativo (`Admin → Importar/Exportar CSV`), usando áreas já semeadas pelo
  `V2__seed_data.sql`.

Diagrama do modelo de dados: [`docs/DATABASE.md`](../docs/DATABASE.md).
