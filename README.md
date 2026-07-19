# Website Pessoal

## Visão Geral

Site pessoal com Angular no front-end e Spring Boot no back-end,
hospedado em uma única instância EC2 com NGINX como proxy reverso.
Autenticação e autorização delegadas ao Keycloak (OIDC/OAuth2).

---

## Stack Tecnológica

| Camada | Tecnologia | Versão atual | Justificativa |
|--------|-----------|-------------|---------------|
| Front-End | Angular | 17+ | Framework completo, TS nativo, DI similar ao Spring |
| Back-End | Spring Boot (Java) | 4.1.0 / Java 21 | Conhecimento prévio, REST simples, ecossistema maduro |
| Banco de Dados | PostgreSQL | 16 | Robusto, JSONB, arrays nativos, compatível com JPA |
| Banco de Dados (testes) | H2 (in-memory) + Testcontainers (PostgreSQL 16) | — | H2 disponível para testes rápidos; integração real via Testcontainers |
| Banco de Dados (produção) | Amazon RDS - PostgreSQL | — | Integra muito bem com instâncias EC2 e aplicações Java |
| ORM | Hibernate (Spring Data JPA) | — | Integrado ao Spring Boot |
| Migrations | Flyway (`spring-boot-starter-flyway`) | — | A partir do Spring Boot 4, o `flyway-core` não migra mais automaticamente; o starter dedicado é necessário. Utilizado para automatizar a criação das tabelas e facilitar a manutenção delas |
| Proxy reverso | NGINX | alpine | Serve estático, faz proxy da API, SSL/TLS, rate limiting |
| Auth | Keycloak | latest | Self-hosted, OAuth2/OIDC, padrão enterprise Java. Servidor de Autenticação e Autorização bem completo |
| Containers | Docker + Docker Compose | — | Todos os serviços em containers. Facilita os testes e deploy |
| Deploy | AWS EC2 | — | Familiaridade com deploy na AWS. Simples de conectar |
| SSL | Let's Encrypt (Certbot) | — | Certificado gratuito renovado automaticamente |
| Utilitários | Lombok | — | Reduz boilerplate (`@Getter`/`@Setter`/`@RequiredArgsConstructor`/`@Slf4j`); excluído do jar final |
| Validação | Jakarta Bean Validation (`spring-boot-starter-validation`) | — | `@NotBlank`, `@Email`, `@Size`, `@URL`, `@Pattern` nos DTOs de request |
| E-mail | Spring Mail (`JavaMailSender`) + `@Scheduled` | — | Envio assíncrono/agendado de notificações de contato, com retry |
| Observabilidade | Spring Actuator | — | Expõe `/actuator/health` (usado pelo `HEALTHCHECK` do Docker) |
| Testes | JUnit 5, Mockito, MockMvc, Testcontainers, Spring Security Test | — | Testes unitários (service) + testes de integração (controller, ponta a ponta com Postgres real) |

---

## Funcionalidades MVP

| Funcionalidade | Endpoints | Auth? | Status |
|---------------|-----------|-------|--------|
| About / Hero | — (dados estáticos) | Não | Não implementado no back-end (estático no front) |
| Portfólio / Projetos | GET público / POST·PUT·DELETE admin | Sim (admin) | ✅ Implementado |
| Skills | GET público / POST·PUT·DELETE admin | Sim (admin) | ✅ Implementado |
| Contato | POST público, com envio de e-mail assíncrono (agendado) | Não | ✅ Implementado |
| Log Interno (diagnóstico) | GET admin (lista dos últimos registros + detalhe por ID) | Sim (admin) | ✅ Implementado |
| Admin Panel | /admin (Angular route) | Sim (admin) | Front-end ainda não versionado neste repositório |

---

## Arquitetura de Sistema

```
Browser (Angular SPA)
    │
    │  HTTPS :443
    ▼
┌──────────── EC2 t3.medium ─────────────────────────────────────────┐
│                                                                      │
│  NGINX (:443 / :80)                                                  │
│    ├── /* ──────────── serve Angular dist (arquivos estáticos)       │
│    ├── /api/* ──proxy──► Spring Boot (:8080)                        │
│    └── /auth/* ─proxy──► Keycloak (:8080)                           │
│                              │              │                        │
│               Spring Boot    │              │  Keycloak              │
│                 ├── JWT validation ◄── JWKS endpoint                │
│                 └── Spring Data JPA                                  │
│                         │                                            │
│                    PostgreSQL (:5432)                                │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
         │
         ├──► SMTP (Gmail / SendGrid) — envio de e-mails
         └──► Browser ◄──► Keycloak  — fluxo OAuth2/OIDC para login
```

### Fluxo de autenticação (admin)

1. Usuário acessa `/admin` no Angular
2. Angular Guard detecta que não há token — redireciona para Keycloak
3. Keycloak autentica (login + senha) e emite JWT
4. Angular armazena o token e reenvia a requisição com `Authorization: Bearer <jwt>`
5. Spring Security valida o JWT consultando o JWKS endpoint do Keycloak
6. Endpoints `POST/PUT/DELETE` verificam o role `ADMIN` no JWT

**Detalhe de implementação — extração das roles do Keycloak:**
o Keycloak não expõe as roles no claim padrão `scope`/`scp` (que o
`JwtAuthenticationConverter` padrão do Spring Security lê), e sim em um claim
customizado `realm_access.roles`. Por isso o projeto define um bean próprio de
`JwtAuthenticationConverter` (em `SecurityConfig`) que lê `realm_access.roles`
e converte cada role em uma `SimpleGrantedAuthority` prefixada com `ROLE_`
(ex.: `ADMIN` → `ROLE_ADMIN`). Sem essa customização, requisições autenticadas
com um token válido do Keycloak retornariam `403 Forbidden`, mesmo tendo a
role correta.

---

## Organização do Código

Esta seção descreve como cada parte do projeto está organizada.

### Front-End (Angular)

O front-end ainda não existe como código neste projeto, mas o plano de organização
segue o padrão comum do Angular, separado em três grandes áreas:

- **`core`** — tudo que é transversal à aplicação e não pertence a nenhuma feature
  específica: o guard de rota que protege `/admin`, o interceptor HTTP que injeta o
  token Bearer nas requisições autenticadas, e os serviços de baixo nível (cliente da
  API, wrapper de autenticação sobre o `keycloak-angular`).
- **`features`** — cada seção pública do site (hero/about, portfólio, skills, contato)
  como um módulo próprio, e uma área `admin` separada para os formulários de
  CRUD protegidos por autenticação.
- **`shared`** — componentes, pipes e diretivas reaproveitáveis entre features.

### Back-End (Spring Boot)

O back-end segue a organização em camadas tradicional do Spring Boot, um pacote por
responsabilidade:

| Pacote | Responsabilidade |
|--------|-------------------|
| `controller` | Recebe as requisições HTTP, delega para o `service` correspondente e não contém regra de negócio |
| `service` | Concentra a regra de negócio; também é onde vivem os jobs `@Scheduled` (envio de e-mail, limpeza de dados antigos) |
| `repository` | Interfaces do Spring Data JPA; queries mais específicas (filas de envio, limpeza) são nativas via `@Query` |
| `model` | Entidades JPA — o mapeamento direto das tabelas do banco |
| `dto` | Payloads de entrada e saída da API, desacoplados das entidades JPA (evita expor a estrutura interna do banco diretamente no contrato de API) |
| `exception` | Exceções de negócio específicas por domínio + o `GlobalBehaviourExceptionHandler`, que centraliza a tradução dessas exceções em respostas HTTP |
| `config` | Configuração de infraestrutura transversal: segurança (Spring Security/OAuth2), CORS, e-mail |

Cada funcionalidade do MVP (Projetos, Skills, Contato, Log Interno) segue esse mesmo
padrão de quatro camadas (`controller` → `service` → `repository` → `model`), o que
torna previsível onde adicionar uma nova funcionalidade no futuro. Os testes seguem a
mesma divisão: testes unitários de `service` (com Mockito) e testes de integração de
`controller` (MockMvc + Testcontainers, subindo um Postgres real).

> O deploy em produção é manual: acesso ao painel da AWS para subir a instância EC2, seguido de acesso via
> **SSH** para baixar o projeto e as dependências e iniciar as aplicações com as instruções de Docker já
> descritas nesta documentação.

---

## API REST — Endpoints

| Método | Endpoint | Descrição | Auth? | Sucesso | Erros possíveis |
|--------|----------|-----------|-------|---------|------------------|
| GET | `/api/projects` | Lista projetos. **Descrição truncada em 50 caracteres** por item (ver `ProjectService.DESCRIPTION_MAX_LENGTH`) | ❌ Público | 200 | — |
| GET | `/api/projects/{id}` | Detalha projeto (descrição completa) | ❌ Público | 200 | 400 (ID não é UUID) · 404 (não encontrado) |
| POST | `/api/projects` | Cria projeto | ✅ ADMIN | 204 | 422 (validação) · 400 (nome duplicado) |
| PUT | `/api/projects/{id}` | Atualiza projeto | ✅ ADMIN | 204 | 400 (ID inválido) · 404 · 422 |
| DELETE | `/api/projects/{id}` | Remove projeto | ✅ ADMIN | 204 | 400 (ID inválido) · 404 |
| GET | `/api/skills` | Lista skills | ❌ Público | 200 | — |
| GET | `/api/skills/{id}` | Detalha skill | ❌ Público | 200 | 400 (ID inválido) · 404 |
| POST | `/api/skills` | Cria skill | ✅ ADMIN | 204 | 422 (validação) · 400 (nome duplicado) |
| PUT | `/api/skills/{id}` | Atualiza skill | ✅ ADMIN | 204 | 400 (ID inválido) · 404 · 422 |
| DELETE | `/api/skills/{id}` | Remove skill | ✅ ADMIN | 204 | 400 (ID inválido) · 404 |
| POST | `/api/contact` | Envia mensagem (persistida; e-mail é enviado depois, de forma assíncrona/agendada — ver seção de Jobs Agendados) | ❌ Público | 201 | 422 (validação) |
| GET | `/api/internal_log` | Lista os 100 registros de log mais recentes (ver seção "Log Interno") | ✅ ADMIN | 200 | — |
| GET | `/api/internal_log/{id}` | Detalha um registro de log pelo ID | ✅ ADMIN | 200 | 400 (ID não é UUID) · 404 (não encontrado) |
| GET | `/actuator/health` | Health check | ❌ Público | 200 | — |

### Padrão de resposta de erro

Todas as exceções de negócio (`ProjectException`, `SkillException`,
`InternalLogException`, `*NotFoundException`) e a exceção genérica (`Exception`) são
centralizadas em `GlobalBehaviourExceptionHandler` (`@RestControllerAdvice`) e
respondem no formato:

```json
{
    "error": "mensagem fixa e genérica sobre a categoria do erro",
    "reason": "mensagem específica do problema"
}
```

Erros de validação de Bean Validation (`@Valid`) retornam `422` no formato:

```json
{
    "message": "The request has failed due to incorrect information in the payload provided.",
    "errors": [
        { "campo": ["mensagem 1", "mensagem 2"] }
    ]
}
```

O handler genérico de `Exception` (`500`) não devolve `exception.getMessage()`
diretamente ao cliente. Em vez disso, cada exceção não tratada é persistida como um
registro de **Log Interno** (ver seção dedicada abaixo) e o campo `reason` retorna uma
mensagem genérica contendo o ID desse registro, que pode ser consultado depois via
`GET /api/internal_log/{id}`.

Exemplo:

```json
{
    "id": "7e5cbeb7-4ef0-4a1c-8488-e38e872f9b13",
    "simpleClassName": "HttpRequestMethodNotSupportedException",
    "errorMessage": "Request method 'PATCH' is not supported",
    "stackTrace": "...",
    "createdAt": "2026-07-18T18:48:09.625254"
}
```

---

## Log Interno (diagnóstico de erros)

O projeto centraliza erros não tratados em uma entidade própria,
`InternalLog`, funcionando como um log de erros simplificado dentro do próprio banco
de dados.

- Toda exceção não tratada que cai no handler genérico do `GlobalBehaviourExceptionHandler`
  gera um registro com o nome da classe da exceção, uma versão truncada da mensagem
  (300 caracteres) e uma versão truncada do stack trace (1000 caracteres). Truncamento
  que existe para não estourar os limites de coluna do banco e para manter os registros
  enxutos.
- Esse mesmo mecanismo é reaproveitado fora do handler genérico: por exemplo, quando o
  envio de e-mail de contato esgota o número de tentativas, esse evento também é 
  registrado como um `InternalLog`, mesmo não sendo uma exceção HTTP.
- A criação do log é resiliente a falhas: se a própria gravação no banco falhar, o
  sistema não propaga um novo erro para o cliente, apenas registra a falha no log da
  aplicação e devolve uma resposta informando que não foi possível salvar o diagnóstico.
- Os registros são acessíveis apenas por administradores, via `GET /api/internal_log`
  (últimos 100 registros) e `GET /api/internal_log/{id}` (detalhe).
- Um job agendado (`@Scheduled`) remove registros com mais de **90 dias**, para o log não
  crescer indefinidamente.

---

## Spring Security

A configuração de segurança (`SecurityConfig`) resolve dois problemas distintos:

1. **Extração das roles do Keycloak.** Como descrito na seção "Fluxo de autenticação"
   acima, o Keycloak expõe as roles em um claim customizado (`realm_access.roles`), e
   não no claim padrão que o Spring Security espera. Por isso existe um bean próprio
   de `JwtAuthenticationConverter` que lê esse claim customizado e converte cada role
   em uma authority no formato que o Spring Security entende (`ROLE_ADMIN`, por
   exemplo).
2. **Regras de autorização por rota.** As leituras públicas (listagem/detalhe de
   projetos e skills, health check, envio de mensagens) são liberadas explicitamente;
   todo o restante exige a role `ADMIN`. Essas regras são definidas de forma
   centralizada no `SecurityFilterChain`, por padrão de URL.

> `@EnableMethodSecurity` está habilitado, mas a autorização hoje é toda feita via
> `requestMatchers` no `SecurityFilterChain` (nível de URL). Nenhum controller usa
> `@PreAuthorize`. A documentação do Spring Boot recomenda evitar duplicar as duas
> abordagens ao mesmo tempo, e como as regras de autorização do projeto ainda são
> simples (baseadas apenas no path do endpoint), a decisão foi manter apenas o
> `requestMatchers`. Se as regras de autorização crescerem em complexidade (ex: um
> mesmo endpoint com regras diferentes por papel/contexto), reconsiderar
> `@PreAuthorize("hasRole('ADMIN')")` nos métodos de escrita para lógica de
> autorização mais complexa.

A URI do JWKS do Keycloak (usada para validar a assinatura dos JWTs) é configurável
via a variável de ambiente `KEYCLOAK_JWKS_URI`.

---

## Schema do Banco de Dados

O schema é versionado pelo Flyway em duas migrations:

- **`V1__initial_schema.sql`** — cria as tabelas `projects`, `skills` e `contact_messages`.
- **`V2__initial_schema.sql`** — adiciona a tabela `internal_log`, usada pelo mecanismo de
  Log Interno.

Tabelas principais:

| Tabela | Finalidade |
|--------|-----------|
| `projects` | Itens do portfólio exibidos no site |
| `skills` | Habilidades/tecnologias exibidas no site, com nível de 1 a 5 |
| `contact_messages` | Mensagens recebidas pelo formulário de contato; `sent` e `retry_count` sustentam o fluxo assíncrono de envio de e-mail |
| `internal_log` | Registros de erro/diagnóstico gerados pela aplicação |

> O `id` de cada entidade é gerado pelo Hibernate em memória (`GenerationType.UUID`)
> antes do INSERT. O `DEFAULT gen_random_uuid()` do Postgres funciona apenas como rede
> de segurança, já que `ddl-auto: validate` não usa os defaults do schema para gerar
> valores.

---

## Envio de E-mail de Contato

A implementação atual **persiste a mensagem imediatamente** e delega o envio do
e-mail de notificação para jobs agendados (`@Scheduled`, habilitados via
`@EnableScheduling` na classe principal). Isso evita que uma falha temporária
de SMTP quebre a requisição do usuário do formulário de contato.

`ContactService` roda três jobs, todos usando queries nativas no
`ContactRepository`:

| Job | Frequência | O que faz |
|-----|-----------|-----------|
| `sendPendingMessages` | a cada 1 hora | Busca mensagens com `sent = false AND retry_count <= 3` e tenta enviar cada uma via `JavaMailSender` |
| `pruneSentMessages` | a cada 30 min | Remove do banco as mensagens já marcadas como `sent = true` |
| `pruneOldMessages` | a cada 24h | Remove mensagens com mais de 14 dias (inclusive as que nunca foram enviadas) |

Ao tentar enviar uma mensagem:
- Sucesso → marca `sent = true` (`markMessageAsSent`)
- Falha (`MailException`) → incrementa `retry_count` (`incrementRetryCount`)

Como a query de busca já filtra `retry_count <= 3`, uma mensagem que falhe 4
vezes deixa de ser buscada pelos próximos ciclos e então é excluída após
14 dias.

Quando uma mensagem esgota as tentativas de reenvio (`retry_count` ultrapassa o
limite), o evento não passa mais despercebido: além de um `log.error` local, um
registro é criado em **Log Interno** com os dados do contato que
falhou, permitindo consultar depois quais mensagens nunca chegaram por e-mail.

## NGINX - Ambiente de DEV

```nginx
# rate=1r/s => 1 request/segundo por IP, em média, para /api/.
limit_req_zone $binary_remote_addr zone=api_limit:10m rate=1r/s;
limit_req_status 429;

server {
    listen 80;
    server_name localhost;

    location /api/ {
        limit_req zone=api_limit burst=20 nodelay;
        proxy_pass         http://backend:8085;
        proxy_set_header   Host              $host;
        proxy_set_header   X-Real-IP         $remote_addr;
        proxy_set_header   X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header   X-Forwarded-Proto $scheme;
    }

    location /actuator/ {
        limit_req zone=api_limit burst=20 nodelay;
        proxy_pass         http://backend:8085;
        proxy_set_header   Host              $host;
        proxy_set_header   X-Real-IP         $remote_addr;
        proxy_set_header   X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header   X-Forwarded-Proto $scheme;
    }

    # Keycloak deste compose roda com KC_HTTP_RELATIVE_PATH=/auth
    location /auth/ {
        limit_req zone=api_limit burst=20 nodelay;
        proxy_pass         http://keycloak:8080;
        proxy_set_header   Host              $host;
        proxy_set_header   X-Real-IP         $remote_addr;
        proxy_set_header   X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header   X-Forwarded-Proto $scheme;
    }

    location = / {
        default_type text/plain;
        return 200 "NGINX dev ativo.\nBack-end:  /api/*  e /actuator/*\nKeycloak:  /auth/*\n";
    }
}
```

---

## Variáveis de Ambiente

### Referência - placeholders lidos pelo `application.yml`

| Variável | Default (se ausente) | Uso |
|----------|----------------------|-----|
| `DB_URL` | `jdbc:postgresql://localhost:5432/website_db` | Datasource |
| `DB_USER` | `website_user` | Datasource |
| `DB_PASS` | `website_pass` | Datasource |
| `KEYCLOAK_JWKS_URI` | `http://localhost:8180/realms/website/protocol/openid-connect/certs` | Validação do JWT |
| `FRONT_END_URL` | `http://localhost:4200` | Origem permitida no CORS (`CorsConfig`) |
| `MAIL_HOST` | `smtp.gmail.com` | SMTP |
| `MAIL_PORT` | `587` | SMTP |
| `MAIL_USER` | `user` | SMTP (usuário e destinatário das notificações de contato) |
| `MAIL_PASS` | `pass` | SMTP |

> ⚠️ Todos os defaults acima existem apenas para não quebrar o boot em dev —
> em produção, **todas** essas variáveis devem ser sobrescritas via `.env` /
> secrets do orquestrador; nenhuma delas deve ficar com o valor padrão.

### .env (EC2, nunca commitar no git)

```dotenv
DB_URL=jdbc:postgresql://postgres:5432/website_db
DB_USER=website_user
DB_PASS=senha_segura_aqui
KEYCLOAK_JWKS_URI=http://keycloak:8080/realms/website/protocol/openid-connect/certs
FRONT_END_URL=https://seusite.dev
KC_ADMIN=admin
KC_ADMIN_PASS=senha_keycloak_aqui
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USER=seu@email.com
MAIL_PASS=app_password_aqui
```

---

## Build - Ambiente de DEV

O `docker-compose.yml` de dev fica em `projetos/` (um nível acima de
`website-backend/`), então os comandos abaixo devem ser executados a partir
dessa pasta.

```bash
cd projetos

# 1. Sobe todo o stack (Postgres, Keycloak, back-end, front-end, NGINX)
docker compose up -d --build

# 2. Alternativa: rodar o back-end fora do Docker (hot reload via devtools)
#    Ainda depende de subir postgres + keycloak (via docker compose up -d postgres keycloak)
cd website-backend/website-backend
./mvnw spring-boot:run -Dspring.profiles.active=dev

# 3. Alternativa: rodar o Angular fora do Docker (hot reload sem rebuild de imagem)
cd website-frontend
ng serve
```

---

## Build e Deploy

Build e Deploy da aplicação Java, Angular e Keycloak.

```bash
# 1. Instalar Docker na EC2
sudo apt update && sudo apt install docker.io docker-compose-plugin -y
sudo usermod -aG docker ubuntu

# 2. Clonar o repositório
# OU subir os arquivos com SCP
git clone https://github.com/usuario/website.git && cd website

# 3. Criar o .env com as variáveis de produção
nano .env

# 4. Build do Angular e copiar dist
cd website-frontend && ng build --configuration production
cp -r dist/ ../frontend/

# 5. Subir tudo
cd .. && docker compose up -d

# 6. Configurar SSL (primeira vez)
docker run --rm -v ./ssl:/etc/letsencrypt -v ./certbot_www:/var/www/certbot \
  certbot/certbot certonly --webroot -w /var/www/certbot \
  -d seusite.dev --email seu@email.com --agree-tos

# 7. Criar realm 'website' e client no Keycloak
# Acesse: https://seusite.dev/auth (após DNS configurado)
```

---

## Pontos de Atenção Conhecidos

Lista consolidada dos pontos levantados na revisão de código, para acompanhamento:


1. **Dockerfile x `application.yml` com porta divergente.** O `EXPOSE` já foi
   corrigido para `8085`, mas o `HEALTHCHECK` ainda bate em
   `http://localhost:8080/actuator/health`. O container continua sendo reportado
   como "unhealthy" pelo Docker até essa linha ser corrigida (**CORRIGIDO**).
2. **`ProjectController` — dupla consulta ao banco em `PUT`/`DELETE`.** Os métodos
   passaram a reaproveitar a mesma entidade `Project` obtida em uma única chamada a
   `service.getProject(id)`, eliminando a segunda ida ao banco e a janela de TOCTOU
   entre as duas leituras (**CORRIGIDO**).
3. **Vazamento de mensagem de exceção no handler genérico (`500`).**
   `GlobalBehaviourExceptionHandler.handleUnhandledException` não devolve mais
   `exception.getMessage()` cru; agora persiste o erro como um registro de Log
   Interno e devolve apenas o ID desse registro no campo `reason` (**CORRIGIDO**).
4. **Arquivo de teste (`config/teste.json`) versionado dentro do código de
   produção** (`src/main/java/.../config/teste.json`), aparentemente esquecido
   ali. Deveria estar em `src/test/resources` ou ser removido (**CORRIGIDO**).
5. **Truncamento de descrição na listagem de projetos não documentado/versionado
   como contrato de API.** `GET /api/projects` corta a descrição em 50
   caracteres sem indicar isso na resposta (sem reticências, sem flag de
   truncamento). O consumidor da API não tem como saber, sem ler este
   documento ou o código, que o campo foi cortado (**CORRIGIDO**).
6. **Sem observabilidade quando uma mensagem de contato esgota as tentativas de
   reenvio de e-mail**. (**CORRIGIDO**).
7. **Rate limit único (`1r/s`) para todas as rotas no NGINX de dev** - mistura
   rotas públicas de leitura com o único endpoint de escrita público
   (`POST /api/contact`) (**MANTER VERSÃO ATUAL**).
8. **`V2__initial_schema.sql` reescreve as tabelas já criadas na `V1`** em vez de
   conter apenas a criação de `internal_log` — ver ressalva na seção "Schema do
   Banco de Dados" acima. Não causa erro hoje (por causa do `IF NOT EXISTS`), mas
   foge do padrão de migrations incrementais e é uma fonte em potencial de
   divergência entre o que está documentado em cada arquivo e o schema real (**MANTER VERSÃO ATUAL**).
