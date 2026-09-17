# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Documentation & Target Architecture

The authoritative design lives in [`docs/`](docs/) (files `1 - Description.md` …
`15 - SDLC & Workflow.md`), which is synced both ways with the GitHub Wiki
(see [`.github/wiki-sync.md`](.github/wiki-sync.md)).

**The docs describe the target; the code is a transitional monolith.** Key gaps to
be aware of:

| Aspect | `docs/` (intended design) | Current code |
| --- | --- | --- |
| Deployment unit | 5 microservices — Core API, Auth, Messaging, Notification, Moderation | single Quarkus module (`fit.biejk`) |
| Async / events | RabbitMQ topic exchange + transactional outbox ([`9 - Event Catalog.md`](docs/9%20-%20Event%20Catalog.md)) | in-process service calls |
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
  - `AuthResource` — Sign-up, sign-in, password reset/change
  - `UserResource` — User profile management (email/password changes)
  - `ClientResource`, `SpecialistResource` — Profile CRUD & search
  - `OrderResource`, `OrderProposalResource` — Job posting & proposal workflows
  - `ReviewResource` — Rating/feedback after order completion
  - `ChatResource` — Chat creation; WebSocket communication in `ChatSocket`
  - `LocationResource`, `ServiceOfferingResource` — Reference data

- **`service/`** — Business logic (one service per domain entity)
  - `AuthService` — Registration, JWT token generation, password recovery flows
  - `UserService` — Profile updates, email/password changes
  - `OrderService`, `ClientService`, `SpecialistService` — CRUD + queries
  - `OrderProposalService` — Proposal lifecycle (create, accept, reject, confirm)
  - `ChatService`, `ChatMessageService` — Chat session & message persistence
  - `MailService` — Email notifications (via Mailtrap in dev)
  - `TokenService` — JWT token validation and claims extraction
  - `SearchService` — Elasticsearch indexing and searching for Orders/Specialists
  - `ReviewService` — Review creation and rating aggregation

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

- **`minIO/`** — File storage integration
  - `FileService` — Upload/download files to MinIO
  - `FileUploadForm` — Multipart form binding

- **`utilits/`** — Shared utilities
  - `CryptoUtils` — Password hashing with BCrypt

#### `/src/main/resources`
Configuration and reference data:
- `application.properties` — Quarkus configuration (DB, JWT keys, Elasticsearch, Redis, MinIO, Mailer)
- `publicKey.pem`, `privateKey.pem` — RSA keys for JWT signing/verification
- `import.sql` — SQL scripts loaded on startup (dev mode with `drop-and-create`)
- `logstash.conf` — Log forwarding config for production

#### `/src/test`
Unit tests (JUnit 5 + REST Assured):
- Test structure mirrors source structure
- Run with `mvn test`

### Configuration & Profiles
- **Dev Profile** (`%dev.*`)
  - Local PostgreSQL, Elasticsearch, Redis, MinIO, Logstash on localhost
  - Database schema dropped and recreated on startup
  - JWT keys embedded in resources
- **Prod Profile** (`%prod.*`)
  - Docker-internal hostnames (db, elasticsearch-search, redis, logstash)
  - MinIO accessible via host.docker.internal from containers
  - Prometheus metrics enabled on `/metrics` endpoint

### Security Model
- **Authentication:** JWT-based using `@JwtAuthentication` interceptor
- **Authorization:** Resource methods use `@RolesAllowed({UserRole.CLIENT})` guards
- **Password Hashing:** BCrypt via `CryptoUtils`
- **API Base Path:** `/api/v1/` for all endpoints

### Key Features & Workflows

#### User Registration & Authentication
1. `AuthResource.signup()` → `AuthService.signup()` → create User/Client/Specialist, hash password
2. `AuthResource.signin()` → generate JWT token containing userId and role
3. JWT claims extracted via `TokenService` in service methods

#### Order Lifecycle (Client → Specialist workflow)
1. Client creates order via `OrderResource.create()` → `OrderService.create()`
2. Specialists browse/search via `SpecialistResource.search()` with Elasticsearch
3. Specialist submits proposal via `OrderProposalResource.create()` → `OrderProposalService.create()`
4. Client reviews proposals, accepts/rejects via `OrderProposalResource.confirmProposal()`
5. Upon acceptance, order status changes to `IN_PROGRESS`
6. Completion marked via `OrderResource.completeOrder()` → triggers review creation

#### Search
- Orders & Specialists indexed in Elasticsearch via `*IndexInitializer`
- Filters support pagination, keyword search, and status/specialty filters
- Index updated on entity create/update via service layer

#### Real-time Chat
- Chat sessions created via `ChatResource.create()`
- WebSocket endpoint at `/ws/chat/{chatId}`
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
