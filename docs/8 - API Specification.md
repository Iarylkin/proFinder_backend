# ProFinder — API Specification

- **Version:** 1.10
- **Date:** 2026-10-27
- **Status:** Stable
- **Purpose:** Every REST endpoint across the 5 services (path, method, auth, request/response shape) plus the Error Catalog those endpoints reference. The Error Catalog lives here (not as its own file) because error codes only exist to serve API responses.

Referenced from [Documentation Roadmap.md](Documentation%20Roadmap.md) (Block B, Tier 1 — "API Specification" + "Error Catalog", now one item). Endpoints are organized by service (Core API, Auth, Messaging, Notification, Moderation), matching [3 - System Design.md](3%20-%20System%20Design.md) and the Controller classes in [7 - Application Classes.md](7%20-%20Application%20Classes.md).

---

## Conventions
---

- **Base path:** every endpoint in this document is relative to **`/api/v1`** (e.g. "Orders" table's `POST /orders` really means `POST /api/v1/orders`) — not repeated on every row to keep the tables scannable. `/v1` doesn't imply a future `/v2` will ever coexist ([API versioning is still "current version only"](2%20-%20Requirements.md) — this is just a stable label, the same way Stripe/GitHub keep `/v1` without running two versions side by side). Since resource names don't collide across services (`orders`/`reviews`/`reports` vs `auth/*` vs `conversations`/`messages` vs `notifications` vs `moderation/*`), Nginx can route by path under the same `/api/v1` prefix without a separate per-service segment.
- **Exception:** the Health endpoints at the bottom of each service section are framework-level (`/q/health/*`, Quarkus `smallrye-health` convention) and are **not** under `/api/v1` — they're infrastructure routes the LB polls directly, not versioned business API.
- **Response envelope (error):** shown below.
- **Pagination:** offset-style, `page`/`size` query params, default 20, `page_size_max` = 100 (matches [FR-Search-10](2%20-%20Requirements.md) and every other "20/page" mention across file 4).
- **Auth:** `Authorization: Bearer <JWT>` header. Endpoints marked "public" need none.
- **Money:** all amounts are plain `numeric`, USD, no `currency` field ([A5 decision](Documentation%20Roadmap.md)).
- **Dates:** ISO 8601 timestamps, no per-user timezone conversion ([A5 decision](Documentation%20Roadmap.md)).
- **Identifiers:** every id is a **UUID v7** (time-ordered). Generated application-side (Hibernate `@UuidGenerator(style = TIME)` or an equivalent v7 generator), never DB-default, so the id exists before flush and travels in the outbox row. v7 over v4 because it sorts by creation time — offset pagination and `ORDER BY created_at` line up with primary-key order, and B-tree index inserts stay near-sequential (far less page fragmentation than random v4). The embedded millisecond timestamp is not considered sensitive (creation time is already exposed as `created_at`).
- **Enums are returned raw, never localized.** The API always emits the machine token — `status: "ACTIVE"`, `reason: "no_show"`, `decision: "content_removed"` — and never a display string. Turning a token into human text is the **client's** job, via its own i18n bundle keyed `enum.<enum_name>.<value>` (e.g. `enum.report_reason.no_show`). MVP ships English only ([NFR-UX-4](2%20-%20Requirements.md)); adding locales later changes nothing in the API contract. This keeps responses locale-independent (so cacheable) and keeps `Accept-Language` out of the service layer. Free-text the server composes for storage — notification `title`/`body` rows — is rendered at write time from server-side templates and is a separate concern from enum values on the wire.
- **Load balancer (decided 2026-09-05):** every service sits behind the Nginx LB from [3 - System Design.md § Nginx Gateway](3%20-%20System%20Design.md) — fixed the "not needed" contradiction in [2 - Requirements.md](2%20-%20Requirements.md) NFR-Scale-1/2. Practically, this means:
  - Every REST endpoint below must stay **stateless** — no server-side session, no sticky-session requirement. This already holds: auth is a locally-verified JWT, rate limits/denylist live in Redis, everything else in PostgreSQL — any instance can serve any request.
  - Each service exposes `GET /q/health/live` and `GET /q/health/ready` (Quarkus `smallrye-health`, no auth) for the LB's health checks — an instance that can't reach PostgreSQL/Redis/MinIO reports `DOWN` so the LB stops routing to it.
  - **Exception: Messaging Service's WebSocket** is a stateful connection — round-robin alone breaks it. **Already implied by [3 - System Design.md § Redis](3%20-%20System%20Design.md)**, which lists "WebSocket state" as a Redis use case: the fix is **Redis Pub/Sub, not LB sticky sessions** — see the Messaging Service section below for the mechanism.


## Endpoints
---

Each Controller class in [7 - Application Classes.md](7%20-%20Application%20Classes.md) becomes one subsection below.

### Core API (port 8080)
---

Every endpoint below requires `Authorization: Bearer <JWT>` unless marked **public**. "Owner" means the authenticated user's id matches the resource's `customer_id`/`professional_id`/`author_id`.

#### Orders
---

| Endpoint | Auth | Request | Response | Errors |
|---|---|---|---|---|
| `POST /orders` | Customer | `OrderCreateRequestDto {category_id, title, description, budget_min?, budget_max?, location, preferred_date?}` | `201 OrderResponseDto` (status `DRAFT`) | `VALIDATION_ERROR`, `EMAIL_NOT_VERIFIED` |
| `PATCH /orders/{id}` | Customer, owner | `OrderUpdateRequestDto` (partial, any field from create) + `version` | `200 OrderResponseDto` | `VALIDATION_ERROR`, `FORBIDDEN`, `NOT_FOUND`, `CONFLICT`, `ORDER_CLOSED` (editing a `CLOSED` order) |
| `POST /orders/{id}/publish` | Customer, owner | — | `200 OrderResponseDto` (status `ACTIVE`) | `ORDER_NOT_DRAFT`, `NOT_FOUND`, `FORBIDDEN`, `EMAIL_NOT_VERIFIED` |
| `POST /orders/{id}/close` | Customer, owner | — | `200 OrderResponseDto` (status `CLOSED`) | `ORDER_ALREADY_CLOSED`, `NOT_FOUND`, `FORBIDDEN` |
| `POST /orders/{id}/extend` | Customer, owner | `{additional_days: int}` | `200 OrderResponseDto` (new `expires_at`) | `VALIDATION_ERROR`, `ORDER_CLOSED`, `NOT_FOUND`, `FORBIDDEN` |
| `DELETE /orders/{id}` | Customer, owner | — | `204` | `ORDER_NOT_DRAFT`, `NOT_FOUND`, `FORBIDDEN` |
| `GET /orders/{id}` | any authenticated | — | `200 OrderResponseDto` | `NOT_FOUND` |
| `GET /orders` | any authenticated | Customer: `?status=` (own orders, any status). Professional: `?category_id=&city=&budget_min=&budget_max=&page=&size=` (browse `ACTIVE` in their own categories/cities) | `200 { items: OrderListItemDto[], page, size, total }` | `VALIDATION_ERROR` |

#### Responses
---

| Endpoint | Auth | Request | Response | Errors |
|---|---|---|---|---|
| `POST /orders/{id}/responses` | Professional, `profile_status=ACTIVE` | `ResponseCreateRequestDto {quote_price, message?}` | `200/201 ResponseDto` (upsert — 201 on first submit, 200 on edit) | `VALIDATION_ERROR`, `ORDER_CLOSED`, `ORDER_EXPIRED`, `PROFILE_INCOMPLETE`, `EMAIL_NOT_VERIFIED`, `NOT_FOUND` |
| `GET /orders/{id}/responses` | Customer, owner | `?sort=newest\|price\|rating&page=&size=` | `200 { items: ResponseDto[] (incl. professional summary), page, size, total }` | `FORBIDDEN`, `NOT_FOUND` |

#### Reviews
---

| Endpoint | Auth | Request | Response | Errors |
|---|---|---|---|---|
| `POST /reviews` | any authenticated | `ReviewCreateRequestDto {reviewee_id, rating, text?, photo_file_ids?[]}` | `200/201 ReviewDto` (upsert, `reviewer_role` derived from caller's own role) | `VALIDATION_ERROR`, `EMAIL_NOT_VERIFIED`, `NOT_FOUND` (reviewee) |
| `PATCH /reviews/{id}` | author | `{rating?, text?}` | `200 ReviewDto` | `VALIDATION_ERROR`, `FORBIDDEN`, `NOT_FOUND` |
| `DELETE /reviews/{id}` | author or Moderator | — | `204` | `FORBIDDEN`, `NOT_FOUND` |
| `GET /users/{id}/reviews` | **public** | `?direction=customer_written\|professional_written&page=&size=` | `200 { items: ReviewDto[], page, size, total }` | `NOT_FOUND` |
| `POST /reviews/{id}/replies` | the reviewed professional, or the original customer replying back | `ReviewReplyCreateRequestDto {text}` | `201 ReviewReplyDto` | `VALIDATION_ERROR`, `FORBIDDEN` (review is `PROFESSIONAL`-direction — no replies allowed), `NOT_FOUND` |

#### Profiles & Categories
---

| Endpoint | Auth | Request | Response | Errors |
|---|---|---|---|---|
| `GET /me/profile` | any authenticated | — | `200 ProfessionalProfileDto \| CustomerProfileDto` | — |
| `PATCH /me/profile` | any authenticated | `ProfileUpdateRequestDto` (role-appropriate fields) | `200` (same shape as GET) | `VALIDATION_ERROR`, `CATEGORY_REQUIRED`, `CITY_REQUIRED` |
| `GET /professionals/{id}` | **public** | — | `200 ProfessionalProfileDto` (public view: bio, categories, cities, rate, experience, portfolio, rating) | `NOT_FOUND` |
| `GET /customers/{id}` | any authenticated | — | `200 CustomerPublicDto` (public view: display name, city, member since — **no** email or phone; there is no customer list endpoint) | `NOT_FOUND` |
| `GET /categories` | **public** | — | `200 CategoryDto[]` (flat list) | — |
| `POST /categories` | Moderator, `SENIOR` | `{name}` | `201 CategoryDto` | `VALIDATION_ERROR`, `FORBIDDEN` |
| `PATCH /categories/{id}` | Moderator, `SENIOR` | `{name}` | `200 CategoryDto` | `VALIDATION_ERROR`, `FORBIDDEN`, `NOT_FOUND` |
| `DELETE /categories/{id}` | Moderator, `SENIOR` | — | `204` | `FORBIDDEN`, `NOT_FOUND`, `CONFLICT` (still referenced by an order or a professional) |
| `POST /me/portfolio` | Professional | multipart: file + `caption?` | `201 PortfolioPhotoDto` | `FILE_TOO_LARGE`, `INVALID_FILE_TYPE`, `STORAGE_UNAVAILABLE`, `VALIDATION_ERROR` |
| `DELETE /me/portfolio/{id}` | Professional, owner, or Moderator | — | `204` | `FORBIDDEN`, `NOT_FOUND` |

#### Reports & Moderation
---

| Endpoint | Auth | Request | Response | Errors |
|---|---|---|---|---|
| `POST /reports` | any authenticated | `ReportCreateRequestDto {reported_user_id, reason, description, evidence?[]}` | `201 ReportDto` | `CANNOT_REPORT_SELF`, `DUPLICATE_REPORT`, `VALIDATION_ERROR`, `NOT_FOUND` |
| `GET /reports` | Moderator | `?status=&page=&size=` | `200 { items: ReportDto[], page, size, total }` | `FORBIDDEN` |
| `POST /reports/{id}/decision` | Moderator | `ReportDecisionRequestDto {decision, suspension_days?, notes?}` | `200 ReportDto` | `VALIDATION_ERROR`, `MODERATION_TARGET_NOT_VERIFIED`, `NOT_FOUND`, `FORBIDDEN` |
| `POST /ban-appeals` | the banned user | `{text?}` | `201 BanAppealDto` | `APPEAL_WINDOW_EXPIRED`, `APPEAL_ALREADY_EXISTS`, `FORBIDDEN` |
| `POST /ban-appeals/{id}/decision` | Moderator (≠ banning moderator) | `{outcome: upheld\|rejected, notes?}` | `200 BanAppealDto` | `SAME_MODERATOR_REVIEW`, `VALIDATION_ERROR`, `NOT_FOUND`, `FORBIDDEN` |
| `POST /users/{id}/block` | any authenticated | — | `204` | `VALIDATION_ERROR` (self-block), `NOT_FOUND` |
| `DELETE /users/{id}/block` | any authenticated | — | `204` | `NOT_FOUND` |

#### Search
---

| Endpoint | Auth | Request | Response | Errors |
|---|---|---|---|---|
| `GET /search/professionals` | **public** | `?q=&category_id=&city=&rating_min=&price_min=&price_max=&experience=&available=&sort=recommended\|rating\|price\|newest\|popularity&page=&size=` | `200 { items: SearchResultCardDto[], page, size, total }` | `SEARCH_UNAVAILABLE`, `VALIDATION_ERROR` |

#### Files
---

| Endpoint | Auth | Request | Response | Errors |
|---|---|---|---|---|
| `POST /files` | any authenticated | multipart: file + `purpose` + `attached_to_id?` | `201 FileUploadResponseDto {id, url}` | `FILE_TOO_LARGE`, `INVALID_FILE_TYPE`, `STORAGE_UNAVAILABLE`, `VALIDATION_ERROR` |
| `GET /files/{id}` | **public** for avatar/portfolio; owner/Moderator for review/report evidence | — | `302` redirect to the MinIO object URL | `NOT_FOUND`, `FORBIDDEN` |
| `DELETE /files/{id}` | owner or Moderator | — | `204` | `FORBIDDEN`, `NOT_FOUND` |

#### Account
---

| Endpoint | Auth | Request | Response | Errors |
|---|---|---|---|---|
| `DELETE /me` | any authenticated | — | `204` (soft-delete: login disabled, profile hidden, PII scrubbed, ACTIVE orders auto-closed; 30-day grace) | `ACCOUNT_DELETION_BLOCKED` |

Reactivation has **no endpoint** — logging in via `POST /auth/login` during the 30-day grace window
silently restores the account (`account.reactivated`). See
[10 - Sequence Diagrams.md § E2](10%20-%20Sequence%20Diagrams.md) and
[4 - Business logic.md § Account Deletion](4%20-%20Business%20logic.md#account-deletion).

#### Health (no auth, used by the load balancer)
---

| Endpoint | Response |
|---|---|
| `GET /q/health/live` | `200` if the JVM is up |
| `GET /q/health/ready` | `200` only if PostgreSQL, Redis, MinIO, and Elasticsearch are all reachable — else `503`, LB stops routing here |


### Auth Service (port 8081)
---

Source: [7 - Application Classes.md § 2. Auth Service](7%20-%20Application%20Classes.md), [4 - Business logic.md § Authentication & Registration](4%20-%20Business%20logic.md#authentication--registration).

#### Registration & Verification
---

| Endpoint | Auth | Request | Response | Errors |
|---|---|---|---|---|
| `POST /auth/register` | **public** | `RegisterRequestDto {email, password}` | `201 {user_id, email, status: PENDING_VERIFICATION}` | `VALIDATION_ERROR`, `EMAIL_ALREADY_REGISTERED` |
| `POST /auth/verify-email` | **public** | `VerifyEmailRequestDto {token}` | `200 {status: ACTIVE}` | `VERIFICATION_TOKEN_INVALID` |
| `POST /auth/resend-verification` | authenticated (`PENDING_VERIFICATION` account) | — (uses JWT `sub`) | `202` (email queued) | `RESEND_TOO_SOON` |
| `POST /auth/forgot-password` | **public** | `ForgotPasswordRequestDto {email}` | `202` — **always**, whether or not the email exists, to avoid leaking which emails are registered | `VALIDATION_ERROR` |
| `POST /auth/reset-password` | **public** | `ResetPasswordRequestDto {token, new_password}` | `200` | `RESET_TOKEN_INVALID`, `VALIDATION_ERROR` |

#### Login & OAuth
---

| Endpoint | Auth | Request | Response | Errors |
|---|---|---|---|---|
| `POST /auth/login` | **public** | `LoginRequestDto {email, password}` | `200 LoginResponseDto {access_token, refresh_token, user: {id, role, status}}` — succeeds even if `SUSPENDED`/`BANNED` (they can still log in to see their status/file an appeal per [4 - Business logic.md § Report Workflow](4%20-%20Business%20logic.md#report-workflow)); logging in while `DELETED` triggers reactivation (`account.reactivated`) | `INVALID_CREDENTIALS`, `ACCOUNT_LOCKED`, `VALIDATION_ERROR` |
| `GET /auth/google/callback` | **public** (OAuth redirect) | `?code=&state=` | `200 LoginResponseDto`, or `200 {role_selection_required: true, temp_token}` on first login with no role yet | `VALIDATION_ERROR`, `GOOGLE_ACCOUNT_ALREADY_LINKED` |
| `POST /auth/google/unlink` | authenticated | — | `204` | `PASSWORD_REQUIRED_TO_UNLINK` |
| `POST /auth/role` | authenticated, no role set yet | `RoleSelectionRequestDto {role: CUSTOMER\|PROFESSIONAL}` | `200` | `VALIDATION_ERROR`, `ROLE_IMMUTABLE` (role already set) |

#### Sessions & Tokens
---

| Endpoint | Auth | Request | Response | Errors |
|---|---|---|---|---|
| `POST /auth/refresh` | **public** (refresh token carries the identity) | `RefreshRequestDto {refresh_token}` | `200 TokenPairResponseDto {access_token, refresh_token}` (rotation — old refresh token invalidated) | `REFRESH_TOKEN_INVALID`, `REFRESH_TOKEN_REUSED` |
| `POST /auth/logout` | authenticated | `{refresh_token}` | `204` (revokes that one token) | — |
| `POST /auth/logout-all` | authenticated | — | `204` (revokes every refresh token + sets the Redis denylist) | — |

#### Health (no auth, used by the load balancer)
---

| Endpoint | Response |
|---|---|
| `GET /q/health/live` | `200` if the JVM is up |
| `GET /q/health/ready` | `200` only if PostgreSQL and Redis are reachable — else `503` |


### Messaging Service (port 8082)
---

Source: [7 - Application Classes.md § 3. Messaging Service](7%20-%20Application%20Classes.md), [4 - Business logic.md § Messaging & Communication](4%20-%20Business%20logic.md#messaging--communication), [3 - System Design.md § Messaging Service ↔ Clients](3%20-%20System%20Design.md).

#### WebSocket ↔ Load Balancer: Redis Pub/Sub, not sticky sessions
---

[3 - System Design.md § Redis](3%20-%20System%20Design.md) already lists "WebSocket state" as a Redis use case — that's the mechanism, made explicit here:

- On connect, an instance subscribes to the Redis Pub/Sub channel `ws:user:{user_id}` for the connecting user. On disconnect, it unsubscribes.
- To deliver a message, **any** instance simply `PUBLISH`es to `ws:user:{recipient_id}` — Redis fans it out to whichever instance(s) currently hold a live subscription for that user, no lookup table needed.
- This also gets multi-device support "for free": [4 - Business logic.md § Sessions & Tokens](4%20-%20Business%20logic.md#sessions--tokens) already allows simultaneous sessions from multiple devices — each device's socket (possibly on different LB-routed instances) just subscribes to the same `ws:user:{id}` channel, and a `PUBLISH` reaches all of them.
- **No LB session affinity is needed.** A reconnect can land on any instance and just re-subscribes.
- **Reconnection / missed messages:** Pub/Sub doesn't replay history — on reconnect the client calls `GET /conversations/{id}/messages?after={last_seen_message_id}` (REST, below) to catch up. This is also the documented WebSocket-unavailable fallback ([3 - System Design.md](3%20-%20System%20Design.md): "Fallback: HTTP polling if WebSocket unavailable").

#### WebSocket Protocol
---

- **Endpoint:** `wss://.../api/v1/ws/messages?access_token={JWT}` — token passed as a query param (browsers can't set an `Authorization` header on the WS upgrade request); validated the same way as REST (local signature check + Redis denylist) before the upgrade completes.
- **Message envelope** (JSON, both directions):

| Direction | Type | Payload |
|---|---|---|
| client → server | `message.send` | `{conversation_id, text}` |
| client → server | `message.delivered` | `{message_id}` — client ack on receipt |
| client → server | `message.read` | `{message_id}` — client ack on read |
| server → client | `message.new` | `{message: MessageDto}` |
| server → client | `message.status` | `{message_id, status: DELIVERED\|READ}` — pushed to the **sender** when the recipient acks |
| server → client | `error` | `{code, message}` — same codes as the REST [Error Catalog](#error-catalog) |

#### REST (history + fallback)
---

| Endpoint | Auth | Request | Response | Errors |
|---|---|---|---|---|
| `GET /conversations` | any authenticated | `?page=&size=` | `200 { items: ConversationDto[], page, size, total }` | — |
| `GET /conversations/{id}/messages` | participant (customer or professional on that conversation) | `?after=&page=&size=` | `200 { items: MessageDto[], page, size, total }` | `FORBIDDEN`, `NOT_FOUND` |
| `POST /conversations/{id}/messages` | participant | `SendMessageRequestDto {text}` — HTTP fallback when WebSocket is unavailable | `201 MessageDto` | `VALIDATION_ERROR`, `USER_BLOCKED`, `FORBIDDEN`, `NOT_FOUND` |
| `DELETE /messages/{id}` | sender | — | `204` (hides immediately in UI; hard-deleted after the moderator-review grace period) | `FORBIDDEN`, `NOT_FOUND` |

Note: `POST /conversations/{id}` to create/get a conversation doesn't exist as its own endpoint — a conversation is implicitly `getOrCreate()`'d the first time a customer messages a professional (via `POST /conversations/{id}/messages` where `{id}` is actually keyed by the `(customer_id, professional_id)` pair, or a dedicated `POST /conversations {professional_id}` starter call — left as an implementation detail, not a design decision, since [4 - Business logic.md § Conversation Model](4%20-%20Business%20logic.md) already settled that a conversation is keyed by the pair, not an order).

#### Health (no auth, used by the load balancer)
---

| Endpoint | Response |
|---|---|
| `GET /q/health/live` | `200` if the JVM is up |
| `GET /q/health/ready` | `200` only if PostgreSQL and Redis are reachable — else `503` |


### Notification Service (port 8083)
---

Source: [7 - Application Classes.md § 4. Notification Service](7%20-%20Application%20Classes.md), [4 - Business logic.md § Notifications](4%20-%20Business%20logic.md#notifications).

#### Notifications & Preferences
---

| Endpoint | Auth | Request | Response | Errors |
|---|---|---|---|---|
| `GET /notifications` | any authenticated | `?is_read=&page=&size=` | `200 { items: NotificationDto[], page, size, total, unread_count }` | — |
| `POST /notifications/mark-read` | any authenticated | `{ids: uuid[]}` | `204` | `VALIDATION_ERROR` |
| `POST /notifications/mark-all-read` | any authenticated | — | `204` | — |
| `GET /me/notification-preferences` | any authenticated | — | `200 NotificationPreferenceDto[]` (the `(event_type, channel, enabled)` matrix; missing cells default `enabled: true`) | — |
| `PATCH /me/notification-preferences` | any authenticated | `[{event_type, channel, enabled}]` | `200` (updated matrix) | `VALIDATION_ERROR` |

#### Device Tokens & Push
---

| Endpoint | Auth | Request | Response | Errors |
|---|---|---|---|---|
| `POST /me/device-tokens` | any authenticated | `DeviceTokenRegisterDto {token, platform}` | `200/201` (upsert on `token`) | `VALIDATION_ERROR` |
| `DELETE /me/device-tokens/{id}` | owner | — | `204` | `FORBIDDEN`, `NOT_FOUND` |

#### Email Delivery
---

| Endpoint | Auth | Request | Response | Errors |
|---|---|---|---|---|
| `GET /unsubscribe` | **public** (token from the email link) | `?token=&event_type=` (`event_type=all` for the global footer link) | `200` (confirmation) | `VALIDATION_ERROR` (invalid/expired token) |

No endpoint triggers a send directly — email/push/in-app delivery is entirely event-driven (RabbitMQ consumers in [7 - Application Classes.md § 4.4 Event Consumption](7%20-%20Application%20Classes.md), not part of the REST surface).

#### Health (no auth, used by the load balancer)
---

| Endpoint | Response |
|---|---|
| `GET /q/health/live` | `200` if the JVM is up |
| `GET /q/health/ready` | `200` only if PostgreSQL and Redis are reachable — else `503` |


### Moderation Service (port 8084)
---

Source: [7 - Application Classes.md § 5. Moderation Service](7%20-%20Application%20Classes.md) — **MVP note applies**: only the sync-check endpoints below are real MVP surface; everything else in [3 - System Design.md § Moderation Service Details](3%20-%20System%20Design.md) (ML toxicity, image classification, `/moderation/analytics`) is Phase 2, not built now.

#### Sync Content Checks (MVP)
---

Internal-only — called server-to-server by Core API before storing a review/message/reply, never exposed through the public load balancer. Auth is network-level (internal VPC / service mesh trust), not an end-user JWT.

| Endpoint | Auth | Request | Response | Errors |
|---|---|---|---|---|
| `POST /moderation/check-text` | internal (Core API only) | `ModerationCheckRequestDto {text, type, user_id}` | `200 ModerationCheckResultDto {approved, checks_passed[], score}` (<200ms) | `VALIDATION_ERROR` |
| `POST /moderation/check-url` | internal (Core API only) | `{url}` | `200 ModerationCheckResultDto` | `VALIDATION_ERROR` |

#### Health (no auth, used by the load balancer)
---

| Endpoint | Response |
|---|---|
| `GET /q/health/live` | `200` if the JVM is up |
| `GET /q/health/ready` | `200` |


## Error Catalog
---

Every error code the API can return, with HTTP status, user-facing message, and trigger condition — collected from scattered mentions across [2 - Requirements.md](2%20-%20Requirements.md) and [4 - Business logic.md](4%20-%20Business%20logic.md). Codes not already fixed in those files were named here for the first time, following the existing `SCREAMING_SNAKE_CASE` style (`EMAIL_NOT_VERIFIED`, `ORDER_CLOSED`, etc.).

### Response Envelope
---

Every error response uses the same shape, regardless of service:

```json
{
  "error": {
    "code": "ORDER_CLOSED",
    "message": "This order is closed",
    "details": null
  }
}
```

- `code` — one of the values below, stable, machine-matchable.
- `message` — human-readable, safe to show in a UI as-is.
- `details` — nullable; for `VALIDATION_ERROR` it's a field→reason map (see [Generic / Cross-Cutting](#generic--cross-cutting)), otherwise usually `null`.


### Generic / Cross-Cutting
---

Apply to every service, not tied to one domain.

| Code | HTTP | Message | Trigger |
|---|---|---|---|
| `VALIDATION_ERROR` | 400 | "Some fields are invalid" | request body fails Jakarta Bean Validation; `details` carries `{field: reason}` |
| `UNAUTHENTICATED` | 401 | "Please log in" | missing/invalid/expired JWT |
| `FORBIDDEN` | 403 | "You don't have permission to do this" | valid token, wrong role/ownership for the action |
| `NOT_FOUND` | 404 | "Not found" | resource id doesn't exist (or is soft-deleted and treated as gone) |
| `CONFLICT` | 409 | "This item was modified, please refresh" | optimistic-locking failure (`version` mismatch) — see [4 - Business logic.md § Concurrency & Edge Cases](4%20-%20Business%20logic.md#concurrency--edge-cases) |
| `RATE_LIMITED` | 429 | "Too many requests, try again later" | any per-IP/per-user/per-endpoint limit from [NFR-Sec-7](2%20-%20Requirements.md); response carries `Retry-After` |
| `INTERNAL_ERROR` | 500 | "Something went wrong, please try again" | unhandled server error |


### Auth Errors
---

| Code | HTTP | Message | Trigger |
|---|---|---|---|
| `INVALID_CREDENTIALS` | 401 | "Incorrect email or password" | login with wrong password, or unknown email |
| `ACCOUNT_LOCKED` | 429 | "Too many failed attempts, try again in 15 minutes" | 5 failed logins in a sliding 15-min window ([FR-Auth-1](2%20-%20Requirements.md)); carries `Retry-After` |
| `EMAIL_NOT_VERIFIED` | 403 | "Please verify your email to do this" | `PENDING_VERIFICATION` account attempts publish/respond/message/review/appear-in-search ([4 - Business logic.md § Email Registration](4%20-%20Business%20logic.md#email-registration)) |
| `EMAIL_ALREADY_REGISTERED` | 409 | "An account with this email already exists" | registration with a duplicate (case-insensitive) email |
| `VERIFICATION_TOKEN_INVALID` | 400 | "This verification link is invalid or expired" | expired (>24h) or already-used verification token |
| `RESET_TOKEN_INVALID` | 400 | "This reset link is invalid or expired" | expired (>15min) or already-used password-reset token |
| `RESEND_TOO_SOON` | 429 | "Please wait before requesting another email" | resend verification faster than 1/60s or beyond 5/hour |
| `GOOGLE_ACCOUNT_ALREADY_LINKED` | 409 | "This Google account is already linked to another user" | strict 1:1 Google↔account link violated |
| `PASSWORD_REQUIRED_TO_UNLINK` | 400 | "Set a password before unlinking Google" | Google-only account tries to unlink without a password set first |
| `ROLE_SELECTION_REQUIRED` | 403 | "Please choose Customer or Professional first" | first Google login, no role chosen yet, action requires a role |
| `ROLE_IMMUTABLE` | 403 | "Role can't be changed — contact support" | attempt to change role via a non-support path ([FR-Auth-3](2%20-%20Requirements.md)) |
| `REFRESH_TOKEN_INVALID` | 401 | "Session expired, please log in again" | refresh token expired, revoked, or unknown |
| `REFRESH_TOKEN_REUSED` | 401 | "Session expired, please log in again" | a rotated (already-used) refresh token is presented again → whole family revoked ([4 - Business logic.md § Sessions & Tokens](4%20-%20Business%20logic.md#sessions--tokens)) |
| `SESSION_REVOKED` | 401 | "Session expired, please log in again" | Redis denylist hit (ban / suspension / password change / role change / logout-all) |


### Orders & Responses Errors
---

| Code | HTTP | Message | Trigger |
|---|---|---|---|
| `ORDER_NOT_DRAFT` | 409 | "This order can no longer be edited this way" | attempting to hard-delete or re-publish a non-`DRAFT` order ([4 - Business logic.md § DRAFT](4%20-%20Business%20logic.md)) |
| `ORDER_CLOSED` | 409 | "This order is closed" | response (or edit) submitted to a `CLOSED` order ([FR-Responses-1](2%20-%20Requirements.md)) |
| `ORDER_EXPIRED` | 409 | "This order has expired" | response submitted to an order past `expires_at` (customer must extend first) |
| `ORDER_ALREADY_CLOSED` | 409 | "This order is already closed" | `closeOrder()` called twice |
| `PROFILE_INCOMPLETE` | 403 | "Complete your profile to do this" | `INCOMPLETE` professional tries to respond/appear in search ([FR-Profile-P5](2%20-%20Requirements.md)) |


### Review Errors
---

No domain-specific codes beyond the generic ones — posting/editing a review is an upsert (one per pair), so there's no "duplicate review" error; replying to a `PROFESSIONAL`-direction review (customer reliability rating) is simply not exposed as an action at all, so it surfaces as `FORBIDDEN` if attempted.


### Profiles & Categories Errors
---

| Code | HTTP | Message | Trigger |
|---|---|---|---|
| `CATEGORY_REQUIRED` | 400 | "You must keep at least one category" | professional tries to remove their last remaining category — replace-only rule ([5 - State Machines.md § Professional Profile](5%20-%20State%20Machines.md)) |
| `CITY_REQUIRED` | 400 | "You must keep at least one city" | professional tries to remove their last remaining city, same rule |


### Reports & Moderation Errors
---

| Code | HTTP | Message | Trigger |
|---|---|---|---|
| `CANNOT_REPORT_SELF` | 400 | "You can't report yourself" | `reporter_id == reported_user_id` ([FR-Reports-1](2%20-%20Requirements.md)) |
| `DUPLICATE_REPORT` | 409 | "You already reported this user today" | second report on the same `(reporter, reported_user)` pair within 24h ([FR-Reports-12](2%20-%20Requirements.md)) |
| `MODERATION_TARGET_NOT_VERIFIED` | 409 | "This account hasn't verified its email yet — suspend/ban isn't available until it does" | moderator tries to suspend/ban a `PENDING_VERIFICATION` account ([5 - State Machines.md § Account Status](5%20-%20State%20Machines.md)) |
| `ACCOUNT_SUSPENDED` | 403 | "Your account is suspended until {date}" | a suspended user attempts any restricted action |
| `ACCOUNT_BANNED` | 403 | "Your account is banned" | a banned user attempts any action except logout / file one appeal |
| `APPEAL_WINDOW_EXPIRED` | 400 | "The 30-day appeal window has passed" | ban appeal filed after `ban_appeal_window_days` ([FR-Reports-13](2%20-%20Requirements.md)) |
| `APPEAL_ALREADY_EXISTS` | 409 | "You've already appealed this ban" | one appeal per ban, second attempt rejected |
| `SAME_MODERATOR_REVIEW` | 403 | "A different moderator must review this appeal" | the banning moderator tries to review their own appeal ([4 - Business logic.md § Ban Appeals](4%20-%20Business%20logic.md#ban-appeals)) |


### Account Deletion Errors
---

| Code | HTTP | Message | Trigger |
|---|---|---|---|
| `ACCOUNT_DELETION_BLOCKED` | 409 | "Can't delete your account while a ban/suspension or open report is active" | self-delete attempted under an active ban/suspension or with an open report ([4 - Business logic.md § Account Deletion](4%20-%20Business%20logic.md#account-deletion)) |


### Search Errors
---

| Code | HTTP | Message | Trigger |
|---|---|---|---|
| `SEARCH_UNAVAILABLE` | 503 | "Search is temporarily unavailable" | Elasticsearch is down, no PostgreSQL fallback ([4 - Business logic.md § Search Failure Handling](4%20-%20Business%20logic.md#search-failure-handling)) |


### File Errors
---

| Code | HTTP | Message | Trigger |
|---|---|---|---|
| `FILE_TOO_LARGE` | 413 | "File exceeds the size limit" | upload exceeds the per-type limit ([4 - Business logic.md § File Storage](4%20-%20Business%20logic.md#file-storage)) |
| `INVALID_FILE_TYPE` | 415 | "This file type isn't allowed here" | magic-byte check fails (wrong type for the upload purpose, or an executable in a message attachment) |
| `STORAGE_UNAVAILABLE` | 503 | "Upload failed, please try again" | MinIO is down — upload fails immediately, no queueing ([4 - Business logic.md § System-Failure Behaviour](4%20-%20Business%20logic.md#system-failure-behaviour)) |


### Messaging Errors
---

| Code | HTTP | Message | Trigger |
|---|---|---|---|
| `USER_BLOCKED` | 403 | "You can't message this user" | sender is blocked by (or has blocked) the recipient — history stays visible, only new messages are prevented ([4 - Business logic.md § Blocking](4%20-%20Business%20logic.md#blocking)) |


### Internal / Infrastructure Failures
---

Not client-facing error *codes* — these describe what happens when a piece of infrastructure itself is down, already specified in [4 - Business logic.md § System-Failure Behaviour](4%20-%20Business%20logic.md#system-failure-behaviour):

| Failure | Behaviour | Surfaces to client as |
|---|---|---|
| RabbitMQ down | Events queue in the outbox, flush on recovery | nothing — business operation still succeeds |
| Elasticsearch down | `503`, no PostgreSQL fallback; reindex events wait in the queue | `SEARCH_UNAVAILABLE` |
| Email (SMTP) down | Notification Service NACKs → RabbitMQ redelivers, 3 retries → `notifications.deadletter` | nothing — in-app notification still succeeds |
| MinIO down | Upload fails immediately, no queueing | `STORAGE_UNAVAILABLE` |

Any other unhandled server exception (a bug, an unexpected null, a driver timeout) is not individually cataloged — it's mapped by `ApiExceptionMapper` (see [7 - Application Classes.md § Shared / Cross-Service Classes](7%20-%20Application%20Classes.md)) to the generic `INTERNAL_ERROR` (500) response, with the real exception logged with a stack trace at `ERROR` level (structured JSON, per [NFR-Mon-6](2%20-%20Requirements.md)). A design-time catalog of internal exception types isn't useful here — that's an implementation detail, not a decision that needs documenting up front.


## Changelog
---

| Version | Date | Change |
|---|---|---|
| 1.10 | 2026-10-27 | `FILE_TOO_LARGE` is now `413` and `INVALID_FILE_TYPE` is `415` (they were `400`), so clients can tell a size error from a type error by the status code. |
| 1.9 | 2026-10-26 | Added `GET /customers/{id}`: a public customer view for any authenticated user, without contact data, and no customer list. |
| 1.8 | 2026-09-10 | Standardised to the shared doc format (metadata, separators, changelog). Added the **`DELETE /me`** account-deletion endpoint (used by files 10, 11, 13 but previously undocumented). `ACCOUNT_DELETION_BLOCKED` status **403 → 409** to match [10 - Sequence Diagrams.md § E2](10%20-%20Sequence%20Diagrams.md) and [13 - Validation Rules.md](13%20-%20Validation%20Rules.md). Wired `GOOGLE_ACCOUNT_ALREADY_LINKED` to the Google-callback endpoint. Removed the stale "Next" section (Event Catalog is done — [9 - Event Catalog.md](9%20-%20Event%20Catalog.md)). |
| 1.7 | 2026-09-07 | Added the **UUID v7** and **enums returned raw** conventions — closed the last two open "API conventions" items. |
| 1.6 | 2026-09-05 | Rebuilt so all 5 services sit correctly under `## Endpoints`, with `## Error Catalog` as its own sibling section. |
| 1.5 | 2026-09-05 | Notification Service + Moderation Service endpoints — API spec now complete for all 5 services. |
| 1.4 | 2026-09-05 | Messaging Service: WebSocket protocol/envelope, Redis Pub/Sub fan-out, REST history/fallback. |
| 1.3 | 2026-09-05 | Auth Service endpoints; `/api/v1` base path. |
| 1.2 | 2026-09-05 | Core API endpoints (Orders, Responses, Reviews, Profiles, Reports, Search, Files, Health). |
| 1.1 | 2026-09-05 | API conventions + full Error Catalog, written as Markdown (not raw OpenAPI YAML). |
| 1.0 | 2026-09-05 | Initial. |
