# MDB Mobile (Android nativo, Java)

Cliente Android **simplificado** do MDB - Math Data Base. Consome diretamente a API núcleo
Java/Spring Boot (`/api/...`). Não reproduz o painel administrativo nem CSV/simulados/sorteio
(esses seguem restritos ao frontend web, conforme o enunciado do projeto).

## Funcionalidades implementadas

- Login e cadastro de usuário.
- Catálogo de problemas com busca por texto e filtros (área, dificuldade mínima/máxima).
- Página do problema: enunciado com renderização de LaTeX (via WebView + MathJax), botões
  "Mostrar dica" e "Mostrar solução" (ocultos por padrão).
- Avaliação de dificuldade (1 a 5) e edição da própria avaliação.
- Favoritar / Quero fazer / Já fiz (toggle).
- Perfil com abas: Favoritos, Quero fazer, Já fiz.

## Arquitetura

- `api/` - `ApiService` (Retrofit), `ApiClient` (instância única + interceptor de token JWT),
  `TokenManager` (sessão em `SharedPreferences`).
- `model/` - DTOs espelhando o JSON retornado pela API Java.
- `ui/` - Activities e o `ProblemAdapter` (RecyclerView).

O app usa **um único backend**: a API Java. O microsserviço Python (CSV, sorteio, simulados)
não é consumido pelo app mobile neste MVP, para manter o cliente simplificado conforme pedido.

## Configuração da URL da API

Definida em `app/build.gradle` via `buildConfigField`:

```groovy
buildConfigField "String", "JAVA_API_BASE_URL", '"http://10.0.2.2:8080"'
```

- `10.0.2.2` é o endereço que o **emulador Android** usa para acessar o `localhost` da máquina host.
- Rodando em um **dispositivo físico**, troque para o IP da máquina na rede local
  (ex.: `http://192.168.0.10:8080`) e garanta que o backend Java aceite conexões externas
  (`SERVER_PORT`/firewall) e que a origem esteja liberada em `CORS_ALLOWED_ORIGINS` se necessário
  (CORS não afeta chamadas nativas Android, apenas WebView/browser).

## Limitação conhecida deste ambiente de desenvolvimento

Este projeto foi gerado em um ambiente **sem Android SDK/Android Studio instalado**, portanto:

- O código-fonte, o Gradle (`build.gradle`, `settings.gradle`) e o manifesto estão completos.
- **O build não pôde ser compilado nem executado neste ambiente.** Não foi gerado nenhum APK e
  nenhum teste instrumentado foi rodado.
- O arquivo binário `gradle/wrapper/gradle-wrapper.jar` não está incluso (não é possível gerar um
  binário via este ambiente). Ao abrir o projeto no **Android Studio**, ele detecta o wrapper
  incompleto e oferece para regenerá-lo automaticamente; alternativamente, com o Gradle instalado
  localmente, rode `gradle wrapper` dentro de `mobile/` uma única vez.

### Como validar localmente

1. Abra a pasta `mobile/` no Android Studio (Giraffe ou mais recente).
2. Deixe o Android Studio sincronizar o Gradle (ele resolve o wrapper e as dependências).
3. Suba o backend Java (`docker compose up backend-java postgres` a partir da raiz do repositório,
   ou `./mvnw spring-boot:run` dentro de `backend-java/`).
4. Rode o app em um emulador Android (API 26+) - a URL `http://10.0.2.2:8080` já aponta para o
   backend local automaticamente.
5. Use as credenciais de desenvolvimento: `admin@mdb.dev` / `admin123` ou `demo@mdb.dev` / `demo123`.
