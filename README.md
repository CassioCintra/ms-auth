# ms-auth

Servidor de autorização OAuth 2.1 + OIDC 1.0 do ecossistema **Switchboard**. Substitui o Keycloak com uma implementação própria baseada em **Spring Authorization Server**, mantendo o ecossistema Java/Spring e dando controle total sobre os claims do JWT.

> **Este serviço gerencia:** emissão e validação de tokens JWT, refresh tokens, sessões, registro de usuários, registro de clients OAuth2 e login.  
> **Os outros serviços validam:** tokens via chave pública JWKS — sem roundtrip ao ms-auth por requisição.

---

## Stack

| Componente     | Tecnologia                         |
|----------------|------------------------------------|
| Linguagem      | Java 25                            |
| Framework      | Spring Boot 4.0.6                  |
| Auth Server    | Spring Authorization Server        |
| Banco de dados | PostgreSQL 16                      |
| Migrations     | Flyway                             |
| Testes         | JUnit 5 · Mockito · Testcontainers |

---

## Arquitetura

O serviço segue **arquitetura hexagonal light**:

```
domain/
  user/    ← AuthUser, UserAlreadyExistsException
  client/  ← Client, ClientNotFoundException

application/
  port/in/   ← RegisterClientUseCase, CreateUserUseCase
  port/out/  ← UserRepository, ClientRepository
  service/   ← ClientService, UserRegistrationService

adapter/
  in/web/
    admin/   ← AdminClientController (POST + DELETE /auth/admin/clients)
    user/    ← UserController (POST /auth/register e /auth/users)
    filter/  ← CorrelatorFilter (X-Correlator + MDC)
  out/
    persistence/
      entity/      ← UserEntity (@ElementCollection para authorities)
      UserJpaRepository, UserPersistenceAdapter
      ClientPersistenceAdapter (via JdbcRegisteredClientRepository)
      AuthUserDetailsService, UserDetailsAdapter

config/
  SecurityConfig         ← filter chains, JWK, JwtDecoder, beans OAuth2
  TokenCustomizerConfig  ← injeta email e name no access token e define o aud pelos escopos (auth.audiences)

infra/
  ClientSeeder  ← registra os 4 clients OAuth2 ao subir (idempotente)
```

### Regra de dependências

```
adapter/in/web  →  port/in  →  service  →  port/out  →  adapter/out/*
domain          ←  nunca importa camadas acima
```

### Dois filter chains

| Order | Chain                                    | Matcher                                                     | Auth                    |
|-------|------------------------------------------|-------------------------------------------------------------|-------------------------|
| 1     | `authorizationServerSecurityFilterChain` | `/oauth2/**`, `/.well-known/**`, `/userinfo`, `/connect/logout` | —                   |
| 2     | `defaultSecurityFilterChain`             | todo o resto                                                | form login + HTTP Basic |

---

## Rodando localmente

### Pré-requisitos

- Java 25
- Docker (PostgreSQL via container)

### 1. Criar o banco de dados

```bash
docker exec -it <id-do-container-postgres> psql -U postgres -c "CREATE DATABASE ms_auth;"
```

Ou via qualquer cliente PostgreSQL:

```sql
CREATE DATABASE ms_auth;
```

### 2. Rodar a aplicação

```bash
./mvnw spring-boot:run
```

O Flyway aplica as migrations automaticamente. Ao subir, o `ClientSeeder` registra os clients OAuth2.

A API ficará disponível em:

```
http://localhost:9000
```

Swagger UI: `http://localhost:9000/swagger-ui.html`

### 3. Criar usuário em desenvolvimento

Não há seeder de usuários — crie via endpoint público:

```bash
curl -s -X POST http://localhost:9000/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","email":"admin@dev.local","password":"senha1234","fullName":"Admin"}'
```

Para promover a `ROLE_AUTH_ADMIN` (acesso ao endpoint admin):

```sql
INSERT INTO authorities (username, authority) VALUES ('admin', 'ROLE_AUTH_ADMIN');
```

---

## Endpoints

### OIDC / OAuth2 (padrão — gerenciados pelo Spring Authorization Server)

| Método | Rota                                | Descrição                                                    | Auth               |
|--------|-------------------------------------|--------------------------------------------------------------|--------------------|
| `POST` | `/oauth2/token`                     | Emite tokens (client_credentials, authorization_code, refresh_token) | client credentials |
| `GET`  | `/oauth2/authorize`                 | Inicia fluxo Authorization Code                              | —                  |
| `GET`  | `/oauth2/jwks`                      | Chave pública RSA para validação offline de JWT              | público            |
| `GET`  | `/.well-known/openid-configuration` | Discovery OIDC                                               | público            |
| `POST` | `/oauth2/introspect`                | Introspecção de token                                        | client credentials |
| `POST` | `/oauth2/revoke`                    | Revogação de token                                           | client credentials |
| `GET`  | `/userinfo`                         | Claims do usuário autenticado (OIDC)                         | Bearer token       |

### Usuários (customizado)

| Método | Rota              | Descrição                                              | Auth                              |
|--------|-------------------|--------------------------------------------------------|-----------------------------------|
| `POST` | `/auth/register`  | Auto-cadastro — qualquer pessoa pode se registrar      | público                           |
| `POST` | `/auth/users`     | Criação interna — fluxo de convite do ms-workspace-management | Bearer (`SCOPE_users:admin`) |

### Admin (customizado)

| Método   | Rota                             | Descrição                   | Auth                          |
|----------|----------------------------------|-----------------------------|-------------------------------|
| `POST`   | `/auth/admin/clients`            | Registra novo client OAuth2 | HTTP Basic (`ROLE_AUTH_ADMIN`) |
| `DELETE` | `/auth/admin/clients/{clientId}` | Revoga/remove um client     | HTTP Basic (`ROLE_AUTH_ADMIN`) |

### Observabilidade

| Método | Rota               | Descrição    | Auth    |
|--------|--------------------|--------------|---------|
| `GET`  | `/actuator/health` | Health check | público |

---

## Como funciona

### Fluxo 1 — Client Credentials (machine-to-machine)

Usado por `order-service`, `ms-workspace-management` e `ms-dashboard`.

```
POST /oauth2/token
  Authorization: Basic <base64(clientId:clientSecret)>
  grant_type=client_credentials&scope=flags:read

  ↓ JdbcRegisteredClientRepository valida clientId + BCrypt(secret)
  ↓ Verifica escopos autorizados para o client
  ↓ Emite JWT assinado com chave RSA em memória
  → { access_token, token_type, expires_in, scope }
```

### Fluxo 2 — Authorization Code + PKCE (frontend)

Usado pelo `dashboard-frontend`.

```
1. Browser → GET /oauth2/authorize?client_id=dashboard-frontend
             &response_type=code&code_challenge=<S256>

2. Spring redireciona para /login (formulário HTML)

3. Usuário envia credenciais → POST /login
   ↓ AuthUserDetailsService.loadUserByUsername()
   ↓ UserRepository → UserPersistenceAdapter → UserJpaRepository
   ↓ BCryptPasswordEncoder.matches(senha, hash)
   ↓ Retorna UserDetailsAdapter (implementa UserDetails)

4. Autenticado → Spring emite authorization_code → redireciona para callback

5. Browser troca code → POST /oauth2/token
   ↓ OAuth2TokenCustomizer injeta email e name no access_token
   → { access_token, refresh_token, id_token, expires_in }
```

> **OAuth 2.1 remove grant_type=password.** Login de usuário é sempre via Authorization Code + PKCE.

### Fluxo 3 — Validação de token pelos demais serviços

Os outros serviços **não chamam o ms-auth** a cada request. Eles validam o JWT localmente:

```
GET /oauth2/jwks  ← chave pública RSA em formato JWK
                  ← obtida uma vez no startup via issuer-uri
                  ← cada serviço verifica a assinatura offline
```

### Fluxo 4 — Registro de usuário

**Auto-cadastro** (chamado pelo dashboard frontend):

```
POST /auth/register                      ← público, sem auth
  { username, email, password, fullName }

  ↓ UserController → CreateUserUseCase
  ↓ UserRegistrationService verifica email duplicado (409 se existir)
  ↓ BCrypt(password) → UserRepository.save()
  → 201 { username, email }
```

**Criação via convite** (chamado pelo ms-workspace-management):

```
POST /auth/users
  Authorization: Bearer <token com scope users:admin>
  { username, email, password, fullName }

  ↓ Spring Resource Server valida Bearer token
  ↓ hasAuthority("SCOPE_users:admin")
  ↓ mesmo fluxo do auto-cadastro
  → 201 { username, email }
```

### Fluxo 5 — Registro de client via endpoint admin

```
POST /auth/admin/clients
  Authorization: Basic <usuario:senha>   ← usuário com ROLE_AUTH_ADMIN
  { clientId, clientSecret, grantTypes, scopes }

  ↓ CSRF ignorado para /auth/admin/** (API REST)
  ↓ AdminClientController → RegisterClientUseCase
  ↓ ClientService → BCrypt(secret) → ClientRepository
  ↓ ClientPersistenceAdapter → JdbcRegisteredClientRepository
  → 201 { id, clientId }
```

### Correlator

O `CorrelatorFilter` intercepta todas as requisições HTTP:

- Lê o header `X-Correlator` (ou gera um UUID se ausente)
- Propaga via MDC — aparece em todos os logs da thread como `[correlator]`
- Devolve o valor no header `X-Correlator` da resposta

---

## Clients OAuth2 (`ClientSeeder`)

Registrados automaticamente ao subir. Idempotente — pula se já existir.

| `clientId`                | Grant type                | Escopos                      | Secret                           |
|---------------------------|---------------------------|------------------------------|----------------------------------|
| `order-service`           | client_credentials        | `flags:read`                 | `order-service-secret`           |
| `ms-workspace-management` | client_credentials        | `users:admin`                | `ms-workspace-management-secret` |
| `ms-dashboard`            | client_credentials        | `flags:read`, `audit:read`   | `ms-dashboard-secret`            |
| `dashboard-frontend`      | authorization_code + PKCE | `openid`, `profile`, `email` | — (PKCE, sem secret)             |

---

## Claims customizados no JWT

Para fluxos com usuário autenticado (`authorization_code`), o `OAuth2TokenCustomizer` injeta:

```json
{
  "sub":   "alice",
  "email": "alice@switchboard.io",
  "name":  "Alice",
  "scope": ["openid", "profile", "email"],
  "iss":   "http://localhost:9000",
  "exp":   1780243813
}
```

Para `client_credentials` (sem usuário), apenas claims padrão OAuth2: `sub`, `aud`, `scope`, `iss`, `exp`.

---

## Migrations Flyway

| Versão | Arquivo                                      | Conteúdo                                                                            |
|--------|----------------------------------------------|-------------------------------------------------------------------------------------|
| V1     | `V1__spring_authorization_server_schema.sql` | Tabelas `oauth2_registered_client`, `oauth2_authorization`, `oauth2_authorization_consent` |
| V2     | `V2__create_users.sql`                       | Tabelas `users` e `authorities`                                                     |

---

## Configuração

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/ms_auth
    username: postgres
    password: postgres

auth:
  issuer-uri: ${AUTH_ISSUER_URI:http://localhost:9000}

server:
  port: 9000
```

> **Atenção — chave RSA:** gerada em memória a cada startup. Tokens emitidos antes de um restart tornam-se inválidos. Para produção, persistir o par de chaves via `KeyStore` ou secret manager.

---

## Testes

```bash
./mvnw test
```

Requer Docker para os testes de integração (Testcontainers sobe PostgreSQL automaticamente).

| Tipo                | Ferramentas                       | Exemplo                        |
|---------------------|-----------------------------------|--------------------------------|
| Web slice           | `@WebMvcTest` + MockMvc           | `UserControllerTest`           |
| Web slice           | `@WebMvcTest` + MockMvc           | `AdminClientControllerTest`    |
| Serviço (unit)      | JUnit 5 + Mockito                 | `ClientServiceTest`            |
| Serviço (unit)      | JUnit 5 + Mockito                 | `UserRegistrationServiceTest`  |
| Adapter (unit)      | JUnit 5 + Mockito                 | `AuthUserDetailsServiceTest`   |
| Filtro (unit)       | `MockHttpServletRequest/Response` | `CorrelatorFilterTest`         |
| Persistência (int.) | Testcontainers (PostgreSQL real)  | `UserPersistenceAdapterTest`   |
| Smoke test          | Testcontainers (PostgreSQL real)  | `MsAuthApplicationTests`       |
| OpenAPI spec        | `@SpringBootTest` + MockMvc + Testcontainers | `OpenApiSpecGeneratorTest` |

```bash
# Gerar spec OpenAPI (requer Docker — usado também pela pipeline de CI)
./mvnw test -Dtest=OpenApiSpecGeneratorTest
```

> Nunca usa H2 — todos os testes de integração rodam contra PostgreSQL real via Testcontainers.

---

## Contexto no ecossistema Switchboard

```
Fase 1 — ms-feature-flags              ✅ concluída
Fase 2 — ms-workspace-management       ✅ concluída
Fase 3 — ms-auth                       ✅ este serviço
Fase 3.1 — Adaptar ms-workspace-management para ms-auth  ✅ concluída
Fase 3.2 — Adaptar ms-feature-flags para ms-auth        ✅ concluída
Fase 4   — Spring Cloud Gateway        ✅ concluída
Fase 4.1 — OpenAPI specs + pipelines   ✅ este serviço incluído
Fase 4.2 — Dockerfiles                 ⏳
Fase 5   — ms-audit                    ⏳
Fase 6   — ms-dashboard (BFF)          ⏳
```

Documentação completa da plataforma: [`platform-ops/docs/architecture/README.md`](../platform-ops/docs/architecture/README.md)
