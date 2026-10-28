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
- **Search:** Elasticsearch 8.15 (indexed documents for Orders & Professionals)
- **Cache:** Redis
- **File Storage:** MinIO (S3-like)
- **Security:** JWT (SmallRye) + BCrypt password hashing
- **Build:** Maven 3.x
- **Deployment:** Docker + Docker Compose

### Project Structure

#### `/src/main/java/fit/biejk`
Main application code organized by layers:

- **`entity/`** — JPA entities (User, Order, Professional, Customer, OrderResponse, Chat, ChatMessage, Review, Category, Location)
  - Every entity extends `BaseEntity`: a `UUID id` (v4, `GenerationType.UUID`) and a `Long version` (`@Version`, optimistic locking; hidden from JSON)
  - Inheritance hierarchy: `User` → `Customer` / `Professional`
  - `OrderStatus`, `OrderResponseStatus`, `UserRole` enums
  
- **`resource/`** — REST endpoints following `/api/v1/{endpoint}` path pattern
  - `AuthResource` — Register (`POST /auth/register`), login (`POST /auth/login`), password recovery (`/password-recovery`)
  - `UserResource` — Avatar upload/download, email/password changes
  - `ProfileResource` — The current user's profile (`GET`/`PATCH /users/me/profile`); picks the customer or professional service and mapper by role
  - `CustomerResource`, `ProfessionalResource` — Profiles by id, own orders; reviews are exposed here (`POST /professionals/{id}/reviews`, `GET .../reviews`, `GET /customers/me/reviews`), responses of a professional (`GET /professionals/{id}/responses`)
  - `ProfessionalSearchResource` — Professional search (`GET /search/professionals`)
  - `OrderResource`, `OrderResponseResource` — Job posting & response workflows (`/orders/{id}/responses`, `/responses/{id}`)
  - `ChatResource` — Chat creation, the current user's chats (`GET /chats/me`) and history; WebSocket communication in `ChatSocket`
  - `LocationResource`, `CategoryResource` — Reference data (`/locations`, `/categories`)

- **`service/`** — Business logic (one service per domain entity)
  - `AuthService` — Registration, JWT token generation, password recovery flows
  - `UserService` — Profile updates, email/password changes
  - `OrderService`, `CustomerService`, `ProfessionalService` — CRUD + queries
  - `OrderResponseService` — Response lifecycle (create, approve; the other responses of the order are rejected)
  - `ChatService`, `ChatMessageService` — Chat session & message persistence
  - `MailService` — Email notifications (via Mailtrap in dev)
  - `TokenService` — JWT token generation (the current user is resolved from the `SecurityIdentity` in `AuthService`)
  - `ReviewService` — Review creation and rating aggregation
  - `LocationService`, `CategoryService` — Reference data

- **`repository/`** — Panache repositories extending `PanacheRepositoryBase<Entity, UUID>`
  - Each entity has a corresponding repository with custom query methods

- **`mapper/`** — MapStruct mappers for DTO ↔ Entity conversion
  - Separate mappers per domain (OrderMapper, CustomerMapper, etc.)

- **`socket/`** — WebSocket endpoint for real-time chat (`ChatSocket`)
  - Message encoding/decoding via `ChatMessageEncoder`/`ChatMessageDecoder`

- **`search/`** — Elasticsearch integration
  - `OrderSearchService`, `ProfessionalSearchService` — Query building & execution
  - `OrderSearchMapper`, `ProfessionalSearchMapper` — DTO mapping for search results
  - `OrderIndexInitializer`, `ProfessionalIndexInitializer` — Index schema setup

- **`dto/`** — Data transfer objects for API requests/responses
  - Paired with `@Valid` validation annotations
  - DTOs: AuthRequest/AuthResponse, OrderDto, UserDto, CustomerDto, ProfessionalDto, PageRequest, filter criteria, etc.

- **`minio/`** — File storage integration
  - `FileService` — Upload/download avatars to MinIO (max 5 MB, type checked by magic bytes)
  - `FileUploadForm` — Multipart form binding
  - `ImageType` — Magic-byte detection of jpeg, png, gif, webp, heic, heif

- **`exception/`** — API errors that carry their own HTTP status (`OrderClosedException` 409, `StatusConflictException` 409, `FileTooLargeException` 413, `InvalidFileTypeException` 415, `InvalidResetCodeException` 400, `TooManyAttemptsException` 429 + `Retry-After`). Standard `BadRequestException`/`ForbiddenException`/`NotFoundException` are used where they fit; `ConcurrentUpdateExceptionMapper` answers `409` when an optimistic lock fails; there is no common error body or `ApiExceptionMapper` yet

- **`utilits/`** — Shared utilities
  - `CryptoUtils` — Password hashing with BCrypt

#### `/src/main/resources`
Configuration and reference data:
- `application.properties` — Quarkus configuration (DB, JWT keys, Elasticsearch, Redis, MinIO, Mailer). Credentials and key paths are read from environment variables (`DB_PASSWORD`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY`, `JWT_PRIVATE_KEY_PATH`, `JWT_PUBLIC_KEY_PATH`); locally they come from `.env` and `keys/`, both git-ignored
- `import.sql` — SQL scripts loaded on startup (`%dev` only, with `drop-and-create`)
- `logstash.conf` — Log forwarding config for production

#### `/src/test`
Unit tests (JUnit 5, Mockito + REST Assured):
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
- **Authorization:** Role guards use string roles, e.g. `@RolesAllowed("CUSTOMER")`; ownership/participation is checked in services (chat participants, order owner, response author)
- **Password Hashing:** BCrypt via `CryptoUtils`
- **API Base Path:** `/api/v1/` for all endpoints

### Key Features & Workflows

#### User Registration & Authentication
1. `AuthResource.register()` (`POST /v1/auth/register`) → `AuthService.register()` → create Customer/Professional by role, hash password
2. `AuthResource.login()` (`POST /v1/auth/login`) → `TokenService.generateToken()` returns a JWT with userId and role
3. The current user ID and role are read from the `SecurityIdentity` via `AuthService.getCurrentUserId()` and `getCurrentUserRole()`; resources and services never use `SecurityIdentity` directly
4. Password recovery (`/v1/auth/password-recovery`): `POST` emails a 6-digit code (always `202`), `PATCH` resets the password; the code is single-use and 5 wrong codes per 15 minutes lock it (`429`)

#### Order Lifecycle (Customer → Professional workflow)
1. Customer creates order via `OrderResource.create()` → `OrderService.create()`
2. Professionals browse orders via `OrderResource.getOrders()` (Elasticsearch when filters are set)
3. Professional submits a response via `OrderResource.respond()` (`POST /v1/orders/{id}/responses`) → `OrderService.respond()`; a closed order returns `409`; the order moves `CREATED` → `CUSTOMER_PENDING`
4. The order owner lists responses (`GET /v1/orders/{id}/responses`) and accepts one via `OrderResponseResource.update()` (`PATCH /v1/responses/{id}` with `{"status": "APPROVED", "finalPrice", "finalDeadline"}`; any other status returns `409`)
5. Upon acceptance, the order status becomes `COMPLETED` and the other responses are rejected (there is no `IN_PROGRESS` status; statuses are `CREATED`, `CUSTOMER_PENDING`, `COMPLETED`, `CANCELLED`)
6. An order is updated with `PATCH /v1/orders/{id}` (`OrderResource.update()`, only the sent fields change); `{"status": "CANCELLED"}` cancels it, any other status or an invalid transition returns `409`, a non-owner gets `403`; a customer can review a professional via `POST /v1/professionals/{id}/reviews`

#### Search
- Orders & Professionals indexed in Elasticsearch via `*IndexInitializer` (the indexes are recreated on startup)
- Keyword and location filters are applied only when provided; orders are searchable by title and description and filterable by `categories`; pagination is 1-based everywhere
- Professionals are searched with `GET /v1/search/professionals` (`ProfessionalSearchResource`)
- Index updated on entity create/update via service layer

#### Real-time Chat
- Chat sessions created via `ChatResource.create()`
- WebSocket endpoint at `/v1/chats/{chatId}?token=<JWT>`; only the two participants of the chat may connect. `chatId` arrives as text and is parsed in `ChatSocket` (WebSocket path parameters cannot be `UUID`)
- Messages encoded/decoded as JSON, persisted to DB and forwarded to connected clients

### Testing & Quality Gates
- **Unit Tests:** Run via `mvn test`
- **Checkstyle:** Enforced by `mvn checkstyle:checkstyle` (Phase: verify)
- **CI Pipeline:** GitHub Actions workflow (`.github/workflows/ci.yml`)
  - Compile → Test → Checkstyle → SonarCloud quality gate
  - Artifacts cached across jobs for performance

### Common Patterns & Conventions

1. **Naming:** Endpoints use snake_case resource names (e.g., `/v1/orders`, `/v1/professionals`)
2. **DTOs:** Always use DTOs for API boundaries; entities marked with `@JsonbTransient` for excluded fields
3. **Validation:** Use Jakarta `@Valid` annotations; validation errors return 400 Bad Request
4. **Exception Handling:** Custom exception handling (if implemented) centralized in resources
5. **Logging:** `@Slf4j` from Lombok for structured logging; never log tokens, passwords or reset codes
6. **Dependency injection:** constructor injection with `@RequiredArgsConstructor` and `private final` fields, no `@Inject` fields
7. **Mappers:** DTO ↔ entity mappers are used in resources, services work with entities. Partial updates (`PATCH`) are passed to the service as an entity that holds only the changes (unsent fields are `null`). Only mappers that build an internal representation (`*SearchMapper`, entity → Elasticsearch document) are used in services
8. **REST style:** state changes are a `PATCH` of the resource (`{"status": ...}`), not verb sub-resources; `/auth/*` actions are the exception. Everything about the current user is under `/users/me/...` (profile) or the resource's own `/me` (`/chats/me`)
9. **ID Mapping:** Resources and services use `AuthService` to get the current user ID (a `UUID`) and role from the JWT claims
10. **Identifiers and concurrency:** every id is a `UUID` and every path variable is a `UUID` (not a valid UUID → `404`); no `Long` ids. `version` is checked only between concurrent transactions on the server: the DTOs do not carry it yet, so a client-sent `version` in `PATCH` (docs/8, docs/13) is planned for 1.6.0

### Debugging & Development Tips

1. **Database Inspection:**
   - Connect to PostgreSQL: `psql -h localhost -U iarylser -d profinder_db`
   - Elasticsearch UI: http://localhost:5602 (Kibana)
   - MinIO Console: http://localhost:9001

2. **Hot Reload:** Changes in `src/main` auto-compile in dev mode; restart WebSocket connections if needed

3. **JWT Debugging:** Tokens include `sub` (userId, a UUID) and `groups` (role); decode at jwt.io for inspection

4. **Elasticsearch Queries:** Verify index mappings and sample documents via Kibana before debugging search logic

5. **Profile Configuration:** Set `quarkus.profile=dev|prod` to switch profiles; defaults to `dev` in IDE

### References
- Quarkus Docs: https://quarkus.io/guides/
- SmallRye JWT: https://smallrye.io/smallrye-jwt/
- Hibernate Panache: https://quarkus.io/guides/hibernate-orm-panache
- Elasticsearch Java Client: https://www.elastic.co/guide/en/elasticsearch/client/java-api-client/current/
