# ProFinder — Application Classes (MVC)

- **Version:** 1.1
- **Date:** 2026-09-10
- **Status:** Stable
- **Purpose:** Every application-layer class to build, grouped by microservice (see [3 - System Design.md](3%20-%20System%20Design.md)) and then by domain module within each service. Layers: **Entity → Repository → Service → Controller → DTO**.

**Conventions:**
- **Entity** rows only name the class and which table it maps to — columns are **not** repeated here, see [6 - Database Schema.md](6%20-%20Database%20Schema.md).
- A join table modeled as a plain `@ManyToMany` / `@ElementCollection` (e.g. `professional_categories`, `professional_cities`) does **not** get its own Entity class — it's a collection field on the owning entity, not a first-class object.
- **Service** rows list the key methods (business behavior — this is the part that doesn't exist anywhere else, unlike Entity/Repository which mirror the DB schema).
- **DTO** rows are named but not field-by-field — fields mirror either the entity (response DTOs) or the request shape implied by the endpoint; full request/response bodies live in [8 - API Specification.md](8%20-%20API%20Specification.md).

---

## 1. Core API (port 8080)
---

Source: [3 - System Design.md § 2. Core API](3%20-%20System%20Design.md), [4 - Business logic.md](4%20-%20Business%20logic.md).

### 1.1 Orders & Responses
---

| Class | Layer | Responsibility |
|---|---|---|
| `Order` | Entity | maps to `orders` (see [file 6](6%20-%20Database%20Schema.md#2-orders--categories)) |
| `Response` | Entity | maps to `responses` |
| `OrderRepository` | Repository | `findActiveByCategoryAndCity()`, `findByCustomer()`, `findExpiring(before)` |
| `ResponseRepository` | Repository | `findByOrder(sort)`, `findByOrderAndProfessional()` (upsert lookup) |
| `OrderService` | Service | `createDraft()`, `updateDraft()` / `updateActive()`, `publish()`, `close()`, `extendExpiration()`, `autoCloseForAccountAction(userId)` — called from `ReportService`/account deletion |
| `ResponseService` | Service | `submitOrEdit()` (one per professional per order, `409 ORDER_CLOSED`/`409 ORDER_EXPIRED` checks), `listForOrder(sortBy)` filtering out banned/suspended/deleted professionals |
| `OrderController` | Controller | `POST /orders`, `PATCH /orders/{id}`, `POST /orders/{id}/publish`, `POST /orders/{id}/close`, `POST /orders/{id}/extend`, `DELETE /orders/{id}` (draft only), `GET /orders/{id}`, `GET /orders` |
| `ResponseController` | Controller | `POST /orders/{id}/responses`, `GET /orders/{id}/responses` |
| `OrderCreateRequestDto`, `OrderUpdateRequestDto`, `OrderResponseDto`, `OrderListItemDto` | DTO | order request/response shapes |
| `ResponseCreateRequestDto`, `ResponseDto` | DTO | response request/response shapes |

### 1.2 Reviews
---

| Class | Layer | Responsibility |
|---|---|---|
| `Review` | Entity | maps to `reviews` (see [file 6](6%20-%20Database%20Schema.md#3-reviews)) |
| `ReviewReply` | Entity | maps to `review_replies` |
| `ReviewRepository` | Repository | `findByReviewerAndReviewee()`, `findByReviewee(direction)` |
| `ReviewReplyRepository` | Repository | `findByReview(review)` |
| `ReviewService` | Service | `postOrEdit()` (upsert one per pair), `delete()`, `recalcRating(revieweeId, direction)` (synchronous, same transaction), `reply()` (rejects replies on `PROFESSIONAL`-direction reviews) |
| `ReviewController` | Controller | `POST /reviews`, `PATCH /reviews/{id}`, `DELETE /reviews/{id}`, `GET /users/{id}/reviews`, `POST /reviews/{id}/replies` |
| `ReviewCreateRequestDto`, `ReviewDto` | DTO | review request/response |
| `ReviewReplyCreateRequestDto`, `ReviewReplyDto` | DTO | reply request/response |

### 1.3 Profiles & Categories
---

| Class | Layer | Responsibility |
|---|---|---|
| `ProfessionalProfile` | Entity | maps to `professional_profiles` (see [file 6](6%20-%20Database%20Schema.md#1-users--auth)) |
| `CustomerProfile` | Entity | maps to `customer_profiles` |
| `Category` | Entity | maps to `categories` — flat list, no `parent` field ([A4](Documentation%20Roadmap.md)) |
| `PortfolioPhoto` | Entity | maps to `portfolio_photos` |
| `ProfessionalProfileRepository`, `CustomerProfileRepository`, `CategoryRepository`, `PortfolioPhotoRepository` | Repository | standard finders |
| `ProfileService` | Service | `completeProfile()`, `updateCategories()` / `updateCities()` — enforces "replace, never clear to zero" ([5 - State Machines.md](5%20-%20State%20Machines.md)), recomputes `INCOMPLETE`→`ACTIVE` |
| `CategoryService` | Service | admin/moderator CRUD on the flat category list |
| `PortfolioService` | Service | `upload()`, `updateCaption()`, `delete()` (own or moderator) |
| `ProfileController` | Controller | `GET/PATCH /me/profile`, `GET /professionals/{id}` |
| `CategoryController` | Controller | `POST/PATCH/DELETE /categories`, `GET /categories` |
| `PortfolioController` | Controller | `POST /me/portfolio`, `DELETE /me/portfolio/{id}` |
| `ProfessionalProfileDto`, `CustomerProfileDto`, `ProfileUpdateRequestDto`, `CategoryDto`, `PortfolioPhotoDto`, `PortfolioUploadRequestDto` | DTO | profile/category/portfolio shapes |

### 1.4 Reports & Moderation
---

| Class | Layer | Responsibility |
|---|---|---|
| `Report` | Entity | maps to `reports` (see [file 6](6%20-%20Database%20Schema.md#6-moderation--reports)) |
| `ReportEvidence` | Entity | maps to `report_evidence` |
| `BanAppeal` | Entity | maps to `ban_appeals` |
| `Block` | Entity | maps to `blocks` |
| `ReportRepository`, `BanAppealRepository`, `BlockRepository` | Repository | `existsRecentReport(reporter, reported, within24h)`, `findQueue(status)`, etc. |
| `ReportService` | Service | `submit()` (1/day check), `decide()` — dispatches to warn/suspend/ban/remove/dismiss, **rejects suspend/ban unless `reported.status == ACTIVE`** ([5 - State Machines.md § Account Status](5%20-%20State%20Machines.md)) |
| `BanAppealService` | Service | `file()` (within 30 days, one per ban), `review()` (enforces reviewer ≠ banning moderator) |
| `BlockService` | Service | `block()`, `unblock()` (not silent — notifies the blocked user) |
| `ReportController` | Controller | `POST /reports`, `GET /reports` (moderator queue), `POST /reports/{id}/decision` |
| `BanAppealController` | Controller | `POST /ban-appeals`, `POST /ban-appeals/{id}/decision` |
| `BlockController` | Controller | `POST/DELETE /users/{id}/block` |
| `ReportCreateRequestDto`, `ReportDto`, `ReportDecisionRequestDto`, `BanAppealCreateRequestDto`, `BanAppealDto`, `BlockDto` | DTO | request/response shapes |

### 1.5 Search
---

No dedicated entities — reads `Order`/`ProfessionalProfile` via their repositories, writes to Elasticsearch, not PostgreSQL.

| Class | Layer | Responsibility |
|---|---|---|
| `ProfessionalSearchDocument` | ES document mapper | the minimal projection served to search cards ([FR-Search-10](2%20-%20Requirements.md)) — not a JPA entity |
| `SearchIndexConsumer` | Service (RabbitMQ `@Incoming`) | consumes `search.professional_reindex` / `search.order_reindex` (see [9 - Event Catalog.md](9%20-%20Event%20Catalog.md)), upserts ES idempotently on `version` |
| `SearchService` | Service | builds filter/sort/`function_score` queries, applies the 60s Redis result-page cache |
| `SearchController` | Controller | `GET /search/professionals` |
| `SearchQueryDto`, `SearchResultCardDto` | DTO | query params / result card shape |

### 1.6 Files
---

| Class | Layer | Responsibility |
|---|---|---|
| `FileAsset` | Entity | maps to `files` (see [file 6](6%20-%20Database%20Schema.md#7-files--portfolio)) |
| `FileAssetRepository` | Repository | standard finders |
| `FileStorageService` | Service | upload to MinIO, magic-byte type validation, EXIF strip, JPEG re-encode/resize per [File Storage rules](4%20-%20Business%20logic.md#file-storage) |
| `FileController` | Controller | `POST /files` (multipart), `GET /files/{id}`, `DELETE /files/{id}` |
| `FileUploadResponseDto` | DTO | uploaded file metadata + URL |

### 1.7 Event Outbox (cross-cutting)
---

| Class | Layer | Responsibility |
|---|---|---|
| `OutboxEvent` | Entity | maps to `outbox` (see [file 6](6%20-%20Database%20Schema.md#8-async--reliability-infra)) |
| `OutboxEventRepository` | Repository | `findUnsent()` |
| `EventPublisher` | Service | injected into every other Core API service — writes the outbox row in the **same transaction** as the business change |
| `OutboxPollerJob` | Service (`@Scheduled`, ~1s) | reads unsent rows, publishes to RabbitMQ, marks sent |

No controller — internal only.

### 1.8 Account
---

| Class | Layer | Responsibility |
|---|---|---|
| `AccountService` | Service | `requestDeletion()` (guard: no active ban/suspension, no open report against the user — else `ACCOUNT_DELETION_BLOCKED`; PII scrub; auto-close ACTIVE orders via `OrderService.autoCloseForAccountAction()`; emit `account.deletion_requested` + reindex), `finalizeDeletion()` (`@Scheduled` — grace window elapsed, emit `account.deletion_finalized`, ES doc hard-purged) |
| `AccountController` | Controller | `DELETE /me` |

Reactivation is **not** here — it happens in the Auth Service's `LoginService` when a `DELETED` account
logs in inside the 30-day grace window (emits `account.reactivated`). See
[10 - Sequence Diagrams.md § E2](10%20-%20Sequence%20Diagrams.md).


## 2. Auth Service (port 8081)
---

Source: [3 - System Design.md § 3. Auth Service](3%20-%20System%20Design.md), [4 - Business logic.md § Authentication & Registration](4%20-%20Business%20logic.md#authentication--registration).

### 2.1 Registration & Verification
---

| Class | Layer | Responsibility |
|---|---|---|
| `User` | Entity | auth-scoped view of `users` (see [file 6](6%20-%20Database%20Schema.md#1-users--auth)) |
| `UserRepository` | Repository | `findByEmail()`, `findByGoogleId()` |
| `RegistrationService` | Service | `registerWithEmail()`, `verifyEmail()`, `resendVerification()` (rate-limited 1/60s, ≤5/hr) |
| `PasswordService` | Service | bcrypt hash/verify, `requestReset()`, `resetPassword()` (15-min link) |
| `AuthController` | Controller | `POST /auth/register`, `POST /auth/verify-email`, `POST /auth/resend-verification`, `POST /auth/forgot-password`, `POST /auth/reset-password` |
| `RegisterRequestDto`, `VerifyEmailRequestDto`, `ForgotPasswordRequestDto`, `ResetPasswordRequestDto` | DTO | request shapes |

### 2.2 Login & OAuth
---

| Class | Layer | Responsibility |
|---|---|---|
| `LoginService` | Service | `login()` — 5-failures/15-min lockout, `429` on trip |
| `OAuthService` | Service | Google code exchange, strict 1:1 link, `unlink()` (requires a password set first), role-selection gate on first login |
| `AuthController` | Controller | `POST /auth/login` |
| `OAuthController` | Controller | `GET /auth/google/callback`, `POST /auth/google/unlink`, `POST /auth/role` |
| `LoginRequestDto`, `LoginResponseDto`, `RoleSelectionRequestDto` | DTO | request/response shapes |

### 2.3 Sessions & Tokens
---

| Class | Layer | Responsibility |
|---|---|---|
| `RefreshToken` | Entity | maps to `refresh_tokens` (see [file 6](6%20-%20Database%20Schema.md#1-users--auth)) |
| `RefreshTokenRepository` | Repository | `findByFamily()`, `findByHash()` |
| `TokenService` | Service | `issueAccessToken()` (15-min JWT, role+status claims), `issueRefreshToken()`, `rotate()`, `detectReuse()` (revokes whole family), `revokeAll()` |
| `SessionDenylistService` | Service | Redis wrapper for `v1:denylist:{user_id}` — set on ban/suspend/role change/password change/logout-all |
| `TokenController` | Controller | `POST /auth/refresh`, `POST /auth/logout`, `POST /auth/logout-all` |
| `RefreshRequestDto`, `TokenPairResponseDto` | DTO | request/response shapes |


## 3. Messaging Service (port 8082)
---

Source: [3 - System Design.md § 4. Messaging Service](3%20-%20System%20Design.md), [4 - Business logic.md § Messaging & Communication](4%20-%20Business%20logic.md#messaging--communication).

### 3.1 Conversations & Messages
---

| Class | Layer | Responsibility |
|---|---|---|
| `Conversation` | Entity | maps to `conversations` (see [file 6](6%20-%20Database%20Schema.md#4-messaging)) |
| `Message` | Entity | maps to `messages` |
| `ConversationRepository`, `MessageRepository` | Repository | `findByPair(customerId, professionalId)`, `findByConversation(paginated)` |
| `ConversationService` | Service | `getOrCreate(customerId, professionalId)` — keyed by pair, not order ([A1 decision](Documentation%20Roadmap.md)) |
| `MessageService` | Service | `send()`, `markDelivered()`, `markRead()`, `delete()` (soft, UI-hides immediately), `purgeExpiredDeleted()` (`@Scheduled`, after grace period) |
| `BlockCheckClient` | Service (HTTP client → Core API) | checks the `blocks` table before allowing a new message |
| `ConversationController` | Controller | `GET /conversations`, `GET /conversations/{id}/messages` |
| `MessageWebSocketEndpoint` | Controller (WebSocket) | connect/auth handshake, send, delivery-receipt events (Sent→Delivered→Read) |
| `MessageController` | Controller | `POST /conversations/{id}/messages` (HTTP fallback), `DELETE /messages/{id}` |
| `ConversationDto`, `MessageDto`, `SendMessageRequestDto`, `MessageReceiptDto` | DTO | request/response shapes |


## 4. Notification Service (port 8083)
---

Source: [3 - System Design.md § 5. Notification Service](3%20-%20System%20Design.md), [4 - Business logic.md § Notifications](4%20-%20Business%20logic.md#notifications).

### 4.1 Notifications & Preferences
---

| Class | Layer | Responsibility |
|---|---|---|
| `Notification` | Entity | maps to `notifications` (see [file 6](6%20-%20Database%20Schema.md#5-notifications)) |
| `NotificationPreference` | Entity | maps to `notification_preferences` |
| `NotificationRepository`, `NotificationPreferenceRepository` | Repository | `findUnread(userId)`, `findByUserAndEventType()` |
| `NotificationCenterService` | Service | `list()`, `markRead()`, `markAllRead()`, `purgeOldRead()` (`@Scheduled`, >90 days) |
| `PreferenceService` | Service | get/update the `(user, event_type, channel)` matrix |
| `NotificationController` | Controller | `GET /notifications`, `POST /notifications/mark-read`, `POST /notifications/mark-all-read` |
| `PreferenceController` | Controller | `GET/PATCH /me/notification-preferences` |
| `NotificationDto`, `NotificationPreferenceDto` | DTO | response/request shapes |

### 4.2 Device Tokens & Push
---

| Class | Layer | Responsibility |
|---|---|---|
| `DeviceToken` | Entity | maps to `device_tokens` |
| `DeviceTokenRepository` | Repository | `findByToken()`, `findStale(60d)` |
| `PushNotificationService` | Service | sends via FCM, deep-link payload `{type, entity_type, entity_id, url}`, deletes token on `UNREGISTERED`/`INVALID` |
| `DeviceTokenService` | Service | `register()`/`refresh()` (`onNewToken`), `invalidate()`, `purgeInactive()` (`@Scheduled`, 60 days) |
| `DeviceTokenController` | Controller | `POST /me/device-tokens`, `DELETE /me/device-tokens/{id}` |
| `DeviceTokenRegisterDto` | DTO | request shape |

### 4.3 Email Delivery
---

| Class | Layer | Responsibility |
|---|---|---|
| `EmailNotificationService` | Service | builds `multipart/alternative` from branded Qute templates, sends via SMTP, real-time (no digest in MVP) |
| `UnsubscribeService` | Service | per-type toggle + "unsubscribe from all" token handling |
| `UnsubscribeController` | Controller | `GET /unsubscribe?token=...` |

No entity — delivery is stateless per event; failures NACK → RabbitMQ redelivery → `notifications.deadletter` after 3 retries.

### 4.4 Event Consumption
---

| Class | Layer | Responsibility |
|---|---|---|
| `OrderEventConsumer`, `MessageEventConsumer`, `ReviewEventConsumer`, `ModerationEventConsumer`, `AccountEventConsumer` | Service (RabbitMQ `@Incoming`) | turn each routing key into a `Notification` row + trigger push/email per preferences |
| `EventDeduper` | Service | idempotency check/write against `processed_events` (dedup by `event_id`) |

No controller — internal consumers only.


## 5. Moderation Service (port 8084)
---

Source: [3 - System Design.md § 5a. Moderation Service](3%20-%20System%20Design.md) — **MVP note applies**: sync checks are the only part that's real MVP behavior; async ML checks are Phase 2.

### 5.1 Sync Content Checks (MVP)
---

| Class | Layer | Responsibility |
|---|---|---|
| `SyncModerationCheckService` | Service | spam-word / profanity-dictionary / URL-blocklist regex checks, <200ms, called synchronously from Core API before storing a review/message |
| `ModerationController` | Controller | `POST /moderation/check-text`, `POST /moderation/check-url` |
| `ModerationCheckRequestDto`, `ModerationCheckResultDto` | DTO | request/response shapes |

### 5.2 Async Checks — Phase 2, not built in MVP
---

`ToxicityChecker`, `ImageClassifier`, `FraudDetector`, the moderation rules engine, and the `/moderation/analytics` endpoint from [3 - System Design.md § Moderation Service Details](3%20-%20System%20Design.md) are **not MVP classes** — [4 - Business logic.md § Moderation & Reports](4%20-%20Business%20logic.md#moderation--reports) confirms MVP moderation is manual/reactive only, handled entirely by `ReportService` in Core API. Listed here only so it's clear they were considered and deliberately deferred, not forgotten.


## 6. Shared / Cross-Service Classes
---

| Class | Present in | Responsibility |
|---|---|---|
| `JwtValidationFilter` | Core API, Messaging Service, Notification Service | verifies the JWT signature **locally** against the Auth Service's public key (no network call), reads `role`/`account_status` claims — see [4 - Business logic.md § Sessions & Tokens](4%20-%20Business%20logic.md#sessions--tokens) |
| `SessionDenylistCheck` | same three services | per-request `GET v1:denylist:{user_id}` in Redis; `401` if present |
| `RateLimitFilter` | all services with public endpoints | Redis-counter based, per-IP (unauth) / per-user (auth) + tighter per-endpoint write caps ([NFR-Sec-7](2%20-%20Requirements.md)) |
| `CorsConfig` | all services | explicit origin allowlist, `Allow-Credentials: false` |
| `ApiExceptionMapper` | all services | maps domain exceptions → HTTP status + error code (the [Error Catalog](8%20-%20API%20Specification.md#error-catalog) in [8 - API Specification.md](8%20-%20API%20Specification.md)) |

**JWT validation:** local signature check against the Auth Service public key (JWKS cached in-process) +
one Redis denylist `GET` per request — no per-request HTTP call to Auth Service.
[3 - System Design.md](3%20-%20System%20Design.md) (v1.3), [4 - Business logic.md § Sessions & Tokens](4%20-%20Business%20logic.md#sessions--tokens),
and [10 - Sequence Diagrams.md § A3](10%20-%20Sequence%20Diagrams.md) all agree on this.


## Changelog
---

| Version | Date | Change |
|---|---|---|
| 1.1 | 2026-09-10 | Standardised to the shared doc format. Added **§1.8 Account** (`AccountService` / `AccountController` for `DELETE /me` — referenced by files 9 & 10 but previously missing). Removed the stale "not-yet-written Error Catalog" wording and the "inconsistency worth flagging" note about JWT validation (file 3 v1.3 resolved it). Removed the stale "Next" section (API Spec / Error Catalog / Event Catalog are all done). |
| 1.0 | 2026-09-04 | Initial. Full Entity/Repository/Service/Controller/DTO breakdown per microservice + a Shared/Cross-Service section. |
