# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Documentation & Target Architecture

The authoritative design lives in [`docs/`](docs/) (files `1 - Description.md` …
`15 - SDLC & Workflow.md`), which is synced both ways with the GitHub Wiki
(see [`.github/wiki-sync.md`](../.github/wiki-sync.md)).

**The docs describe the target; the code is a transitional monolith.** Key gaps to
be aware of:

| Aspect | `docs/` (intended design) | Current code |
| --- | --- | --- |
| Deployment unit | 5 microservices — Core API, Auth, Messaging, Notification, Moderation | single Quarkus module (`fit.biejk`) |
| Async / events | RabbitMQ topic exchange + transactional outbox ([`9 - Event Catalog.md`](../docs/9%20-%20Event%20Catalog.md)) | in-process service calls |
| Domain language | `Customer` / `Professional` | `Client` / `Specialist` entities |
| Schema management | Flyway migrations | Hibernate `drop-and-create` (dev) |

When the code and `docs/` disagree, `docs/` is the intended design and the code is
work in progress. The sections below describe the **code as it is today**.

## Build & Test Commands

### Build
```bash
mvn -B clean test-compile -Dmaven.test.skip=true  # Compile only
mvn -B clean package -DskipTests                   # Full build without tests
```

### Testing
```bash
mvn -B test                                         # Run unit tests
mvn test -Dtest=YourTestClass                     # Run single test class
mvn test -Dtest=YourTestClass#testMethod          # Run specific test method
```

### Code Quality
```bash
mvn -B checkstyle:checkstyle                       # Run Checkstyle checks
```

### Start Development Server
```bash
./mvnw quarkus:dev                                # Hot-reload dev mode
```

### Infrastructure
```bash
cp .env.example .env                              # Local secrets (git-ignored), fill in the values
./scripts/generate-dev-keys.sh                    # Throw-away JWT key pair into keys/ (git-ignored)
docker-compose up -d                              # Start all services (PostgreSQL, Elasticsearch, Redis, MinIO, Kibana)
```

## Codebase Architecture

### Tech Stack
- **Framework:** Quarkus (Java 21) with RESTful API
- **Database:** PostgreSQL with Hibernate ORM + Panache
- **Search:** Elasticsearch 8.15 (indexed documents for Orders & Specialists)
- **Cache:** Redis
- **File Storage:** MinIO (S3-like)
- **Security:** JWT (SmallRye) + BCrypt password hashing
- **Build:** Maven 3.x
- **Deployment:** Docker + Docker Compose

### Project Structure

#### `/src/main/java/fit/biejk`
Main application code organized by layers:

- **`entity/`** — JPA entities (User, Order, Specialist, Client, OrderProposal, Chat, ChatMessage, Review, ServiceOffering, Location)
  - Inheritance hierarchy: `User` → `Client` / `Specialist`
  - `OrderStatus`, `ProposalStatus`, `UserRole` enums
  
- **`resource/`** — REST endpoints following `/api/v1/{endpoint}` path pattern
  - `AuthResource` — Sign-up, sign-in, password recovery (`/password-recovery`)
  - `UserResource` — Avatar upload/download, email/password changes
  - `ClientResource`, `SpecialistResource` — Profile CRUD & search; reviews are exposed here (`POST /specialists/{id}/reviews`, `GET .../reviews`, `GET /clients/me/reviews`)
  - `OrderResource`, `OrderProposalResource` — Job posting & proposal workflows
  - `ChatResource` — Chat creation and history; WebSocket communication in `ChatSocket`
  - `LocationResource`, `ServiceOfferingResource` — Reference data

- **`service/`** — Business logic (one service per domain entity)
  - `AuthService` — Registration, JWT token generation, password recovery flows
  - `UserService` — Profile updates, email/password changes
  - `OrderService`, `ClientService`, `SpecialistService` — CRUD + queries
  - `OrderProposalService` — Proposal lifecycle (create, accept, reject, confirm)
  - `ChatService`, `ChatMessageService` — Chat session & message persistence
  - `MailService` — Email notifications (via Mailtrap in dev)
  - `TokenService` — JWT token generation (the current user is resolved from the `SecurityIdentity` in `AuthService`)
  - `ReviewService` — Review creation and rating aggregation
  - `LocationService`, `ServiceOfferingService` — Reference data

- **`repository/`** — Panache repositories extending `PanacheRepository<Entity, ID>`
  - Each entity has a corresponding repository with custom query methods

- **`mapper/`** — MapStruct mappers for DTO ↔ Entity conversion
  - Separate mappers per domain (OrderMapper, ClientMapper, etc.)

- **`socket/`** — WebSocket endpoint for real-time chat (`ChatSocket`)
  - Message encoding/decoding via `ChatMessageEncoder`/`ChatMessageDecoder`

- **`search/`** — Elasticsearch integration
  - `OrderSearchService`, `SpecialistSearchService` — Query building & execution
  - `OrderSearchMapper`, `SpecialistSearchMapper` — DTO mapping for search results
  - `OrderIndexInitializer`, `SpecialistIndexInitializer` — Index schema setup

- **`dto/`** — Data transfer objects for API requests/responses
  - Paired with `@Valid` validation annotations
  - DTOs: AuthRequest/AuthResponse, OrderDto, UserDto, ClientDto, SpecialistDto, PageRequest, filter criteria, etc.

- **`minio/`** — File storage integration
  - `FileService` — Upload/download avatars to MinIO (max 5 MB, type checked by magic bytes)
  - `FileUploadForm` — Multipart form binding
  - `ImageType` — Magic-byte detection of jpeg, png, gif, webp, heic, heif

- **`exception/`** — API errors that carry their own HTTP status (`OrderClosedException` 409, `FileTooLargeException` 413, `InvalidFileTypeException` 415, `InvalidResetCodeException` 400, `TooManyAttemptsException` 429 + `Retry-After`). Standard `BadRequestException`/`ForbiddenException`/`NotFoundException` are used where they fit; there is no common error body or `ExceptionMapper` yet

- **`utilits/`** — Shared utilities
  - `CryptoUtils` — Password hashing with BCrypt

#### `/src/main/resources`
Configuration and reference data:
- `application.properties` — Quarkus configuration (DB, JWT keys, Elasticsearch, Redis, MinIO, Mailer). Credentials and key paths are read from environment variables (`DB_PASSWORD`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY`, `JWT_PRIVATE_KEY_PATH`, `JWT_PUBLIC_KEY_PATH`); locally they come from `.env` and `keys/`, both git-ignored
- `import.sql` — SQL scripts loaded on startup (`%dev` only, with `drop-and-create`)
- `logstash.conf` — Log forwarding config for production

#### `/src/test`
Unit tests (JUnit 5 + REST Assured):
- Test structure mirrors source structure
- Run with `mvn test`

### Configuration & Profiles
- **Dev Profile** (`%dev.*`)
  - Local PostgreSQL, Elasticsearch, Redis, MinIO, Logstash on localhost
  - Database schema dropped and recreated on startup, `import.sql` loaded, SQL logged
  - Swagger UI at `/q/swagger-ui` (included in dev/test builds only)
- **Prod Profile** (`%prod.*`)
  - Docker-internal hostnames (db, elasticsearch-search, redis, logstash)
  - MinIO accessible via host.docker.internal from containers
  - Prometheus metrics enabled on `/metrics` endpoint
  - Schema generation `none` (the schema must already exist until Flyway arrives); secrets come from the environment, JWT keys from the paths in `JWT_*_KEY_PATH`

### Security Model
- **Authentication:** JWT verified by SmallRye JWT; endpoints are protected with `@Authenticated` / `@RolesAllowed`
- **Authorization:** Role guards use string roles, e.g. `@RolesAllowed("CLIENT")`; ownership/participation is checked in services (chat participants, order owner, proposal author)
- **Password Hashing:** BCrypt via `CryptoUtils`
- **API Base Path:** `/api/v1/` for all endpoints

### Key Features & Workflows

#### User Registration & Authentication
1. `AuthResource.signUp()` (`POST /v1/auth/sign-up`) → `AuthService.signUp()` → create Client/Specialist by role, hash password
2. `AuthResource.signIn()` (`POST /v1/auth/sign-in`) → `TokenService.generateToken()` returns a JWT with userId and role
3. The current user ID is read from the `SecurityIdentity` via `AuthService.getCurrentUserId()`
4. Password recovery (`/v1/auth/password-recovery`): `POST` emails a 6-digit code (always `202`), `PATCH` resets the password; the code is single-use and 5 wrong codes per 15 minutes lock it (`429`)

#### Order Lifecycle (Client → Specialist workflow)
1. Client creates order via `OrderResource.create()` → `OrderService.create()`
2. Specialists browse orders via `OrderResource.getOrders()` (Elasticsearch when filters are set)
3. Specialist submits a proposal via `OrderResource.proposal()` (`POST /v1/orders/{id}/proposals`) → `OrderService.proposal()`; a closed order returns `409`; the order moves `CREATED` → `CLIENT_PENDING`
4. The order owner lists proposals (`GET /v1/orders/{id}/proposals`) and accepts one via `OrderProposalResource.confirm()` (`POST /v1/proposals/{id}/confirm`)
5. Upon acceptance, the order status becomes `COMPLETED` and the other proposals are rejected (there is no `IN_PROGRESS` status; statuses are `CREATED`, `CLIENT_PENDING`, `COMPLETED`, `CANCELLED`)
6. An order can be cancelled via `OrderResource.cancel()` (`POST /v1/orders/{id}:cancel`); a client can review a specialist via `POST /v1/specialists/{id}/reviews`

#### Search
- Orders & Specialists indexed in Elasticsearch via `*IndexInitializer` (the indexes are recreated on startup)
- Keyword and location filters are applied only when provided; orders are searchable by title and description; pagination is 1-based everywhere
- Index updated on entity create/update via service layer

#### Real-time Chat
- Chat sessions created via `ChatResource.create()`
- WebSocket endpoint at `/v1/chats/{chatId}?token=<JWT>`; only the two participants of the chat may connect
- Messages encoded/decoded as JSON, persisted to DB and forwarded to connected clients

### Testing & Quality Gates
- **Unit Tests:** Run via `mvn test`
- **Checkstyle:** Enforced by `mvn checkstyle:checkstyle` (Phase: verify)
- **CI Pipeline:** GitHub Actions workflow (`.github/workflows/ci.yml`)
  - Compile → Test → Checkstyle → SonarCloud quality gate
  - Artifacts cached across jobs for performance

### Common Patterns & Conventions

1. **Naming:** Endpoints use snake_case resource names (e.g., `/v1/orders`, `/v1/specialists`)
2. **DTOs:** Always use DTOs for API boundaries; entities marked with `@JsonbTransient` for excluded fields
3. **Validation:** Use Jakarta `@Valid` annotations; validation errors return 400 Bad Request
4. **Exception Handling:** Custom exception handling (if implemented) centralized in resources
5. **Logging:** `@Slf4j` from Lombok for structured logging
6. **ID Mapping:** Services use `TokenService` to extract current user ID from JWT claims

### Debugging & Development Tips

1. **Database Inspection:**
   - Connect to PostgreSQL: `psql -h localhost -U iarylser -d profinder_db`
   - Elasticsearch UI: http://localhost:5602 (Kibana)
   - MinIO Console: http://localhost:9001

2. **Hot Reload:** Changes in `src/main` auto-compile in dev mode; restart WebSocket connections if needed

3. **JWT Debugging:** Tokens include `sub` (userId) and `groups` (role); decode at jwt.io for inspection

4. **Elasticsearch Queries:** Verify index mappings and sample documents via Kibana before debugging search logic

5. **Profile Configuration:** Set `quarkus.profile=dev|prod` to switch profiles; defaults to `dev` in IDE

### References
- Quarkus Docs: https://quarkus.io/guides/
- SmallRye JWT: https://smallrye.io/smallrye-jwt/
- Hibernate Panache: https://quarkus.io/guides/hibernate-orm-panache
- Elasticsearch Java Client: https://www.elastic.co/guide/en/elasticsearch/client/java-api-client/current/
