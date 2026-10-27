# ProFinder — Refactoring Roadmap

- **Version:** 1.6
- **Date:** 2026-10-27
- **Status:** Draft
- **Purpose:** Step-by-step plan for moving the transitional monolith (`1.0.0`) to the target design in `docs/` (five services, event-driven, Flyway-managed schema), with a version number for every step, the docs fixes each milestone depends on, and the open decisions that block specific versions.

**Sources:** gap analysis of the code against [2 - Requirements.md](2%20-%20Requirements.md), [3 - System Design.md](3%20-%20System%20Design.md), [4 - Business logic.md](4%20-%20Business%20logic.md), [5 - State Machines.md](5%20-%20State%20Machines.md), [6 - Database Schema.md](6%20-%20Database%20Schema.md), [7 - Application Classes.md](7%20-%20Application%20Classes.md), [8 - API Specification.md](8%20-%20API%20Specification.md), [9 - Event Catalog.md](9%20-%20Event%20Catalog.md), [10 - Sequence Diagrams.md](10%20-%20Sequence%20Diagrams.md), [11 - Permission Matrix.md](11%20-%20Permission%20Matrix.md), [12 - Elasticsearch Mapping.md](12%20-%20Elasticsearch%20Mapping.md), [13 - Validation Rules.md](13%20-%20Validation%20Rules.md), [14 - Category Taxonomy.md](14%20-%20Category%20Taxonomy.md), [15 - SDLC & Workflow.md](15%20-%20SDLC%20%26%20Workflow.md).

**Limits of the analysis:** coverage was checked by reading, not by automated tracing. `src/test`, `Dockerfile.jvm`, `dependabot.yml` and `checkstyle.xml` were not inspected. Keep a traceability matrix next to this file and update it as versions ship.

---

## 1. Principles
---

| Rule | Description |
|---|---|
| Version | One version for the whole repository: `pom.xml` = git tag `vX.Y.Z` = Docker tag of every service |
| Release | One PR `develop → main`, one tag, release notes |
| Working version | Every tag starts in compose, passes tests and checkstyle, and passes the smoke scenario (sign up → sign in → create order → respond → accept → chat → complete → review) |
| Compatibility | The old API keeps working through deprecated aliases until `2.0.0` |
| Unfinished work | Hidden behind feature flags, never left half-done in a tagged build |
| Major / minor / patch | `2.0.0` marks the architecture change (five services). Features after it are `2.x` minors. Bug fixes are patches |
| Schema | Flyway from `1.6.0`; every version that changes the schema ships its own migration |
| Docs tasks | Marked `D-…`. They belong to the milestone that needs them and do not bump the version. "Before code" means the task is closed before the first version of that milestone starts |
| Tooling tasks | Marked `T-…`. Repository tooling, no version bump |

## 2. Overview
---

| # | Milestone | Versions | Goal |
|---|---|---|---|
| M0 | Hotfixes | 1.0.1 – 1.0.4 | Close security holes and runtime bugs before any refactoring |
| M1 | Foundation | 1.1.0 – 1.8.0 | Cross-cutting changes needed before splitting |
| M2 | Modular monolith | 1.9.0 – 1.14.0 | Service boundaries inside one process |
| M3 | Safety net | 1.15.0 – 1.17.0 | Tests before splitting |
| M4 | Events & broker | 1.18.0 – 1.21.0 | Outbox, events, RabbitMQ |
| M5 | Service extraction | 2.0.0-rc.1 – 2.0.0 | Physical split into five services |
| M6 | Auth features | 2.1.0 – 2.3.0 | Verification, sessions, OAuth |
| M7 | Core domain | 2.4.0 – 2.8.0 | Categories, profiles, orders, responses, reviews |
| M8 | Notification features | 2.9.0 – 2.10.0 | Preferences, in-app, push |
| M9 | Messaging & files | 2.11.0 – 2.12.0 | Chat rules, files |
| M10 | Rate limiting | 2.13.0 | Per-user and per-endpoint limits |
| M11 | Moderation (in Core) | 2.14.0 – 2.15.0 | Reports, decisions, appeals |
| M12 | Search | 2.16.0 – 2.17.0 | Elasticsearch mapping and search API |
| M13 | Account lifecycle | 2.18.0 | Account deletion |
| M14 | Schema audit | 2.19.0 | Final schema check |
| M15 | Hardening & release | 2.20.0 – 2.23.0 | Load, security, deployment, docs, GA |
| M16 | Patches | x.y.z | Ongoing bug fixes |

---

## M0 — Hotfixes (1.0.1 – 1.0.4)
---

Tag `v1.0.0` on the current state as the rollback base.

| Version / task | What we do |
|---|---|
| **1.0.1** | Remove JWTs, reset codes, passwords and chat content from logs. Override `toString` on DTOs that carry passwords |
| **1.0.2** | Check chat membership (REST and WS), check order ownership for proposals, close the public `/clients` list; `GET /clients/{id}` stays for authenticated users with a public DTO without email and phone; remove the account email from public specialist DTOs (phone stays public per `docs/4`; a separate public contact email comes with 2.5.0). Foreign chat or proposal returns `403 FORBIDDEN`, a missing one `404 NOT_FOUND` per `docs/8` |
| **1.0.3** | `findBySpecialistId`, `createAt` sort in `ChatMessageRepository`, 1-based pagination everywhere, `signUp` with an unknown role, null-safe specialist search, index `title` and `description` of orders, response to a closed order |
| **1.0.4** | Password reset: `SecureRandom` code, single-use, 5 wrong codes per 15 minutes then `429` + `Retry-After` (Redis `reset_fail:{email}`), normalized email, `202` always without revealing whether the email exists, `400` for a wrong or expired code. Avatars: 5 MB limit (`413`), type by magic bytes (`415`). `drop-and-create`, `import.sql` and SQL logging only in `%dev`, `%prod` schema generation `none`, Swagger not in `%prod`. Credentials and JWT keys move to the environment (`.env.example`, `scripts/generate-dev-keys.sh`) |
| **D-0.1** | `docs/15`: describe the `hotfix/*` and `release/x.y` branches this plan uses. Remove the steps that push straight to `develop` and `main` (they contradict branch protection) |
| **T-0.2** | Hooks in `.claude/settings.json`: replace relative paths with absolute ones so they do not break outside the repository root. Fix `CLAUDE.md` (there is no `IN_PROGRESS` status and no `completeOrder()`) |
| Outside versions | Rotate secrets (DB and MinIO passwords, Mailtrap, `privateKey.pem`) and move them to environment variables. Needs action from the owner |

## M1 — Foundation (1.1.0 – 1.8.0)
---

| Version / task | What we do |
|---|---|
| **D-1.1** (before code) | **Done 2026-10-27:** one PostgreSQL, one schema per service, no cross-schema reads (`docs/3`, `6`, `7`). Decision 7. Still open: rewrite `docs/10` B1, E1, E2 without cross-service SQL |
| **D-1.2** (before code) | **Done 2026-10-27:** `display_name` added to `RegisterRequestDto`; `users.role` nullable until a Google sign-up picks one; `POST /auth/role` takes the limited `temp_token`. Fixed in `docs/6`, `8`, `13` |
| **D-1.3** | **Done 2026-10-27:** `quote_price`, `city` (`orders.location` renamed), reviews `direction=CUSTOMER\|PROFESSIONAL`, `UPPER_SNAKE_CASE` enum values everywhere, `ORDER_ALREADY_CLOSED` merged into `ORDER_CLOSED`, `ACCOUNT_LOCKED` and `RATE_LIMITED` kept distinct, Jackson instead of JSON-B, all DTOs become records with `toString` overridden where they carry secrets. Mapper impact is checked in 1.7.0 |
| **D-1.4** | **Done 2026-10-27:** `docs/6` has the `CHECK`s, `citext`, § Foreign Keys & ON DELETE, `professional_profiles.last_active_at` and a partial unique index on live reviews; `docs/14` seeds categories with UUID v7 literals |
| **1.1.0** | `Client → Customer`: entity, repository, service, resource, DTOs, mappers, ES documents, tests |
| **1.2.0** | `Specialist → Professional`, roles `CUSTOMER`/`PROFESSIONAL` in the JWT |
| **1.3.0** | New paths with deprecated aliases for all of them: `/customers`, `/professionals`, `/auth/register`, `/auth/login`, `PATCH /orders`, `/orders/{id}/close`, `/orders/{id}/responses`, `/categories`, `/conversations`, `/me/profile`, `/search/professionals` |
| **1.4.0** | UUID v7 (generated in the application), `version` for optimistic locking. *Decision 3* |
| **1.5.0** | `User` → account + `customer_profiles` and `professional_profiles`; `citext`, `CHECK`, "Key Indexes" for these tables; drop `JOINED` inheritance |
| **1.6.0** | **Flyway:** baseline `V1` taken from the model after 1.5.0; folders `db/migration/{core,auth,messaging,notification,moderation}/`, one history table and one schema per module; `validate`. Dev data lives in `db/dev`, not `import.sql` |
| **1.7.0** | `ApiExceptionMapper`, one error format, Error Catalog, DTO validation per `docs/13` (except trimming passwords and tokens), pagination clamp, Spotless in CI. **Serialization moves from JSON-B (`quarkus-rest-jsonb`, `@JsonbTransient`) to Jackson (`quarkus-rest-jackson`); DTOs move from Lombok `@Data` to `record`s** (immutable); records with secrets (passwords, codes, tokens) override `toString` or use a masked `Secret` type, `@ToString.Exclude` from 1.0.1 is dropped with `@Data`. Check MapStruct mappers (`@MappingTarget`), `@Builder` and annotations on getters first |
| **1.8.0** | Health (`/ready` checks PostgreSQL, Redis, MinIO, ES), Redis 7, profile cleanup, Mailpit in dev compose, `SETUP.md` and `CONFIGURATION.md` |

## M2 — Modular monolith (1.9.0 – 1.14.0)
---

| Version / task | What we do |
|---|---|
| **D-2.1** (before code) | `docs/3`: separate the roles. Reports, appeals and blocks live in Core; the Moderation Service only serves `check-text` and `check-url`. Decisions 2 and 8. Remove "Get chat history for order" |
| **1.9.0** | Packages `fit.biejk.{core,auth,messaging,notification,moderation}`; move classes only |
| **1.10.0** | Auth boundary: `AuthService`, `TokenService`, `CryptoUtils`, `CurrentUser` interface |
| **1.11.0** | Messaging boundary: `Chat`, `ChatMessage`, `ChatSocket`, `MessagingApi` |
| **1.12.0** | Notification boundary: `MailService` behind `NotificationApi` |
| **1.13.0** | Moderation: `check-text` and `check-url` interface with word lists (internal, no public path). `Report` and `Block` skeleton in Core |
| **1.14.0** | Cross-module relations replaced with IDs. ArchUnit tests forbid extra dependencies |

## M3 — Safety net (1.15.0 – 1.17.0)
---

| Version / task | What we do |
|---|---|
| **D-3.1** | `docs/2` and `15`: clarify the coverage gate (JaCoCo 80% vs "not a gate"), "at least one test per endpoint", CI under 5 minutes for five services |
| **1.15.0** | Testcontainers (PostgreSQL, Elasticsearch, Redis, MinIO), Flyway migrations run in tests, `%test` profile for CI |
| **1.16.0** | E2E smoke scenario as its own CI job, remove `continue-on-error` |
| **1.17.0** | Role and authorization tests per `docs/11`, Sonar coverage threshold, OWASP Dependency-Check, SQLi and XSS tests |

## M4 — Events & broker (1.18.0 – 1.21.0)
---

| Version / task | What we do |
|---|---|
| **D-4.1** (before code) | `docs/9`: events `user.unblocked`, report and appeal lifecycle, ban lifted, a mechanism for removing content (`message`, `review_reply`, content type `review_photo`); fix the reindex version when an account status changes |
| **D-4.2** | `docs/7`: add `EventPublisher` and a poller for Auth and Messaging, `OrderExpirationJob`, `MessagePurgeJob`, `ReviewReplyService`, `ProcessedEvent`, token entities; align method names with `docs/9` |
| **1.18.0** | Event model per `docs/9`, `EventPublisher`, outbox + relay with `SELECT … FOR UPDATE SKIP LOCKED`, retention (`sent` older than 7 days), partial index, `processed_events` |
| **1.19.0** | Flows moved to events: `SearchIndexConsumer` builds the document from PostgreSQL, external version, 409 ignored; emails; `message.sent`, `response.created`, `review.posted`. Direct calls removed, ES errors no longer lost |
| **1.20.0** | RabbitMQ in compose: `profinder.events`, `profinder.dlx`, five `*.notifications` queues with a 24 h TTL, `search.index` with no TTL, `notifications.deadletter`, 3 retries, `requeue=true` |
| **1.21.0** | Contract tests for all 29 event payloads; broker flag on by default |

## M5 — Service extraction (2.0.0-rc.1 – 2.0.0)
---

| Version / task | What we do |
|---|---|
| **D-5.1** (before code) | Denylist owner (Auth or Core), decision 6. Recommendation: Core publishes `moderation.banned`, Auth consumes it and sets the denylist |
| **D-5.2** (before code) | How Notification gets the recipient's email and the recipients of `order.published` (decision 9; a projection built from events needs new events in `docs/9`) |
| **D-5.3** | Deployment: VPS + Compose or Kubernetes (decision 10); make `docs/3` and `15` agree. Scaling: change NFR-Scale-1/2 in `docs/2` to one instance at launch |
| **D-5.4** | `docs/15`: update workflow names, paths, the `fit.biejk` package, Mailpit, commit format; API deprecation policy (aliases until `2.0.0`); wiki sync (currently one-way, action pinned to `@main`) |
| **D-5.5** | Availability and latency targets: one set of values in `docs/2` (uptime, search under 2 s vs under 500 ms, cache TTL) |
| **rc.1** | Multi-module Maven, `contracts` (events, DTOs, errors). Monorepo layout `services/<name>` |
| **rc.2** | Notification: own schema, `MailService`, recipient projection |
| **rc.3** | Auth: JWKS endpoint, private key only here, own outbox and poller, `JwtValidationFilter` and denylist check in the other services, JWKS cache |
| **rc.4** | Messaging: own outbox and poller, Redis Pub/Sub instead of a static map, local blocks table fed by `user.blocked` events |
| **rc.5** | Moderation Service: `POST /moderation/check-text` and `/check-url` (under 200 ms), internal access only |
| **rc.6** | Nginx: routing, per-IP rate limiting, TLS (Let's Encrypt), `Connection: upgrade` for WS, health checks; one compose file; CI/CD, GHCR and one image per service; update required checks in branch protection |
| **rc.7** | Prometheus and Grafana dashboards per service, JSON logs with trace id, PII masking, 30-day log retention, alerts, cross-service E2E, timeouts and retries |
| **2.0.0** | Remove deprecated aliases, final smoke test, release |

## M6 — Auth features (2.1.0 – 2.3.0)
---

| Version / task | What we do |
|---|---|
| **D-6.1** (before code) | `docs/4`, `11`, `13`: the `account_status` claim in the JWT can be stale for up to 15 minutes, so decide how the token is refreshed; denylist by `iat`, not by `user_id`; bcrypt truncates at 72 bytes; do not trim passwords and tokens; the ASCII-only name regex rejects names like "José" |
| **D-6.2** | Contact with support for banned and suspended users: a channel and an endpoint, or an explicit "out of MVP" (decision 11); response shape of `GET /me/profile` for `MODERATOR` |
| **2.1.0** | Registration with email verification (24 h link, resend 1/60 s and at most 5/h), email normalization, disposable-domain blocklist, password policy, account statuses, one status gate on every request |
| **2.2.0** | Access 15 min + refresh 7 days with rotation and reuse detection, 30-day cap, denylist, lockout 5/15, logout and logout-all, role chosen once, password reset by a 15-minute link, email and password change with session revocation |
| **2.3.0** | Google OAuth 1:1, unlink (only when a password is set), set password, `ROLE_SELECTION_REQUIRED` and `temp_token` |

## M7 — Core domain (2.4.0 – 2.8.0)
---

| Version / task | What we do |
|---|---|
| **D-7.1** (before code) | `docs/4`, `5`, `11`: draft visibility; `extend` counts from `now` or from the old `expires_at`, and what happens to an expired order; whether the category of an `ACTIVE` order with responses can change; time zones (UTC, whose "today"); customer profile completeness (FR-Auth-4 vs UC-4) |
| **D-7.2** | `docs/4`, `14`: deleting a category referenced by `CLOSED` orders (currently impossible forever); one set of bounds for `hourly_rate` and `years_experience`; meaning of `available` (hides from search or only ranks lower) |
| **D-7.3** | Outlier-filter algorithm, source and recalculation of `m` in the Bayesian sort, rules for reviewing blocked, suspended, deleted and `INCOMPLETE` users, the "Self-Employed Discovery" scenario |
| **2.4.0** | Categories per `docs/14` (32 seeded), cities as strings (normalized), one category per order; `category.*` events |
| **2.5.0** | Profiles: professional (`INCOMPLETE → ACTIVE`, sticky, `years_experience`, `hourly_rate`, hours, `available`) and customer (city, `preferred_category_ids`, bio, avatar); public contact email; `@Version`; `v1:profile:*` cache with delete-on-write |
| **2.6.0** | Order lifecycle `DRAFT → ACTIVE → CLOSED`, publish, close, extend (1–90 days), `expires_at`, `published_at`, `closed_at`, `closed_reason`, expiry scheduler, visibility for professionals (their categories and cities), customer history, `@Version` → 409. *Decision 1* |
| **2.7.0** | Responses: upsert, `edited`, sealed bids, sorting, `ORDER_CLOSED`/`ORDER_EXPIRED`/`PROFILE_INCOMPLETE`, professional-category check |
| **2.8.0** | Reviews in both directions, one per pair, replies (only on `CUSTOMER`-direction reviews), `deleted_at`, `is_outlier`, `rating_avg` and `review_count`, Bayesian sort, customer reliability rating; synchronous `check-text` call. Review photos arrive in 2.12.0 |

## M8 — Notification features (2.9.0 – 2.10.0)
---

| Version / task | What we do |
|---|---|
| **D-8.1** (before code) | `docs/4`, `9`: fan-out rules for `order.published` (category AND city, `ACTIVE` only, `available`, push cap); preference exceptions (`response.edited` in-app only, `auth.*` email only and not switchable) |
| **2.9.0** | `notification_preferences`, in-app store with pagination and mark-read, Qute email templates, unsubscribe in every email, matching-order fan-out, purge of read items older than 90 days, `EventDeduper`, in-app written first |
| **2.10.0** | FCM push, `device_tokens`, delete on `UNREGISTERED` and after 60 days of inactivity, deep links |

## M9 — Messaging & files (2.11.0 – 2.12.0)
---

| Version / task | What we do |
|---|---|
| **D-9.1** (before code) | Chat attachments: `docs/2` (MVP) vs `docs/4` (later), decision 4; chat retention (while the order is active vs until deletion); who may open a conversation (professional or only the customer) |
| **D-9.2** | `docs/13` §9, `docs/4`: HEIC/HEIF and animated GIF on re-encoding, "quality 82" for PNG and WebP, `Cache-Control: immutable` on private evidence, evidence types (`text/photo/message_link` vs files) |
| **2.11.0** | Chat: conversation only between customer and professional, text up to 200 characters, WS protocol per `docs/8`, `GET /conversations`, backfill `?after=`, HTTP fallback, SENT/DELIVERED/READ statuses, deletion (kept 2 days for moderators), purge job, blocks and a notice to the blocked user |
| **2.12.0** | Files and portfolio: `files`, `FileController` with `purpose`, 302 to MinIO, magic bytes, limits, EXIF stripping, resize (avatar 512×512 + 128×128), `Content-Disposition: attachment`, immediate MinIO deletion; review photos, report evidence; chat attachments per decision |

## M10 — Rate limiting (2.13.0)
---

| Version / task | What we do |
|---|---|
| **D-10.1** | `docs/4`, `7`: who owns the configuration registry ("admin-configurable" with no runtime settings in the MVP) and where the limits live |
| **2.13.0** | `RateLimitFilter` on Redis in every service: per-user 1000/h; order 20/h, response 60/h, message 120/h, review 20/h, upload 30/h, report 1/day; `429` + `Retry-After`; Nginx stays per-IP |

## M11 — Moderation, in Core (2.14.0 – 2.15.0)
---

| Version / task | What we do |
|---|---|
| **D-11.1** (before code) | `docs/4`, `5`, `11`, `13`: content identifier for `content_removed`; endpoint for `submitted → under_review` and concurrent decisions by two moderators; a report against a `PENDING_VERIFICATION` user (closing deadline); abuse of "an open report blocks deletion"; effect of a block on responses; contradiction between "moderator contacts a party" and the missing channel |
| **D-11.2** (before code) | Appeals: may a `SENIOR` review a ban they issued; manual unban (`docs/11`) vs the only `BANNED → ACTIVE` transition; moderator deleting orders (decision 5); support tickets and moderation reports (decision 11); audit log for reading private messages |
| **2.14.0** | Role `MODERATOR` + `moderator_level`, moderator account creation, reports (7 reasons, at most 1 per day per pair, `report_evidence`), shared queue, decisions (dismiss, warning with counter, suspend 1–365, ban, content_removed), cascades, automatic return from suspension, `PENDING_VERIFICATION` protected from bans, access to case data and any conversation, deletion of messages and photos, audit log. *Decision 2* |
| **2.15.0** | Appeals (within 30 days, one per ban, a different moderator), appeal list and detail, manual unban, lifecycle events, category management for `SENIOR`, deletion of problematic orders. *Decisions 2 and 5* |

## M12 — Search (2.16.0 – 2.17.0)
---

| Version / task | What we do |
|---|---|
| **D-12.1** (before code) | `docs/4`, `12`: definition of "activity" and `popularity` (needs `last_active_at` and data), `experience` filter ("=" or "≥"), whether the `orders` index is needed at all, `city_normalizer` rules |
| **2.16.0** | Mapping per `docs/12`: `professionals` and `orders`, `dynamic: strict`, analyzers `text_en`/`name_analyzer`/`city_normalizer`, `*_write` aliases, external version, `es/*.json` files, `bayes_prior_*` in config; the index is no longer recreated on startup |
| **2.17.0** | `GET /search/professionals`: filters, sorts, 60 s cache, `503 SEARCH_UNAVAILABLE`, `ACTIVE` only; professionals browse `ACTIVE` orders through the `orders` index; hourly sweep of expired orders and a nightly reindex at about 03:00; fix N+1 in the ES mappers |

## M13 — Account lifecycle (2.18.0)
---

| Version / task | What we do |
|---|---|
| **D-13.1** (before code) | `docs/4`, `5`, `11`: how reactivation by login works when the email is erased immediately; clearing PII in Messaging and Notification (only ES cleanup is described today) |
| **2.18.0** | Account deletion: saga across services, soft delete, PII scrub, 30-day grace, reactivation by login, finalization job, ES removal, "Deleted user" in chat, auto-close of `ACTIVE` orders, blocked during an active ban or an open report |

## M14 — Schema audit (2.19.0)
---

| Version / task | What we do |
|---|---|
| **D-14.1** | Bring `docs/6` in line with the final schema: indexes for jobs and queues (`orders(expires_at)`, `messages(deleted_at)`, `notifications(is_read, created_at)`, `users(status, suspension_end_date)`, `reports(status, created_at)`, `files(attached_to_type, attached_to_id)` and others), duplicated fields (`suspension_end_date`, `ban_appeals.user_id`), link between review photos and `files` |
| **2.19.0** | Audit every service schema against `docs/6`; check indexes and constraints; squash migrations if needed |

## M15 — Hardening & release (2.20.0 – 2.23.0)
---

| Version / task | What we do |
|---|---|
| **D-15.1** | Clean up NFRs and acceptance criteria in `docs/2`: "55+ FR", "7 filters", "10 use cases", outdated extensions, Admin analytics with "revenue", PostgreSQL encryption at rest (PostgreSQL has no built-in TDE) |
| **2.20.0** | Load tests for the main scenarios (one instance per service), timeout tuning |
| **2.21.0** | Security review (`/security-review`), CORS allowlist and security headers, secrets outside the repository, Dependabot, privacy and ToS pages |
| **2.22.0** | Staging environment, auto-deploy on merge to `develop`, post-deploy smoke test, PostgreSQL, MinIO and ES backups with restore verification, `DEPLOYMENT.md` |
| **2.23.0** | Final `docs/` sync through `/sync-docs`, runbook, `TESTING.md`, `CONTRIBUTING.md`, `TROUBLESHOOTING.md`, ADRs, Component, Deployment and Activity diagrams. **Release-ready build** |

## M16 — Patches (ongoing)
---

| Rule | Description |
|---|---|
| Versions | Inside the current line: `1.9.1`, `2.0.1`, `2.6.1` |
| Critical bugs | Cherry-pick from `main` into `release/x.y` when `main` has moved on |
| Priority | Issues labelled `bug`; ranked above the next minor version when the bug breaks the working-version criteria |

---

## 3. Open decisions
---

| # | Question | Needed by |
|---|---|---|
| 3 | UUID v7 or `Long` | 1.4.0 (**first**) |
| 7 | ~~Shared database or schema per service~~ **Decided 2026-10-27:** one PostgreSQL, one schema per service, no reads across schemas | D-1.1 |
| 8 | Reports and appeals in Core (as `docs/` say) | D-2.1 |
| 6 | Who owns the denylist: Core or Auth | D-5.1 |
| 9 | How Notification gets email and recipients (recommended: projection built from events) | D-5.2 |
| 10 | Deployment: VPS + Compose or Kubernetes | D-5.3 |
| 1 | Accepting a professional: change the code to match `docs/`, or change `docs/` to match the code | 2.6.0 |
| 4 | Chat attachments | D-9.1 |
| 2 | Moderation Service in `2.0.0` | D-11.2 |
| 5 | Moderators deleting orders | D-11.2 |
| 11 | Moderation reports, support tickets, Admin analytics: in scope or Phase 2 | D-6.2, D-11.2 |

## 4. Known risks
---

| Risk | Mitigation |
|---|---|
| Splitting on the old data model is painful | UUID and the `User` split come before the split (1.4.0–1.5.0); cross-module relations become IDs in 1.14.0 |
| No test net during the split | M3 is entirely about the net and runs before M4 and M5 |
| Features are slower to build in a distributed system | One compose file, cross-service E2E in rc.7, a shared `contracts` module |
| Schemas keep changing in `2.x` | Flyway from 1.6.0, one migration per version, per-service folders ready for extraction |
| No user-facing features before `2.0.0` | M0 closes the critical holes; M1–M5 change shape only |
| Duplicated background jobs if replicas are added later | `SKIP LOCKED` in the outbox (1.18.0), Redis Pub/Sub for WS (rc.4) |

## Changelog
---

| Version | Date | Change |
|---|---|---|
| 1.0 | 2026-10-26 | Initial. Milestones M0–M16, versions 1.0.1–2.23.0, docs and tooling tasks per milestone, open decisions, risks. |
| 1.1 | 2026-10-26 | DTOs move to records in 1.7.0; D-1.3 covers the decision. |
| 1.6 | 2026-10-27 | D-1.4 closed: schema constraints, `citext`, `ON DELETE`, `last_active_at`, UUID v7 seed. |
| 1.5 | 2026-10-27 | D-1.3 closed: naming, enum case, error codes, Jackson, records. |
| 1.4 | 2026-10-27 | D-1.1 and D-1.2 closed; decision 7 recorded. |
| 1.3 | 2026-10-27 | 1.0.4 row detailed: attempt limit, `413`/`415` for avatars, `%prod` schema `none`, secrets moved to the environment. |
| 1.2 | 2026-10-26 | 1.0.2: public DTOs drop the account email only (phone stays public per `docs/4`); error codes follow `docs/8`. |
