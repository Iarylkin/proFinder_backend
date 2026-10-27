# ProFinder — Sequence Diagrams

- **Version:** 1.1
- **Date:** 2026-09-10
- **Status:** Stable
- **Purpose:** Mermaid `sequenceDiagram` for every cross-service flow that isn't obvious from a single endpoint — the choreography between Client, the 5 services, and the infrastructure (PostgreSQL, Redis, Elasticsearch, MinIO, RabbitMQ, FCM, SMTP, Google). [Documentation Roadmap.md](Documentation%20Roadmap.md) Block C "Sequence" row.

**Sources:** [3 - System Design.md](3%20-%20System%20Design.md), [4 - Business logic.md](4%20-%20Business%20logic.md), [7 - Application Classes.md](7%20-%20Application%20Classes.md), [8 - API Specification.md](8%20-%20API%20Specification.md), [9 - Event Catalog.md](9%20-%20Event%20Catalog.md).

**Conventions**
- `LB` = Nginx load balancer / gateway. Omitted from diagrams where it only proxies.
- `MQ` = RabbitMQ (`profinder.events` topic exchange). Every publish shown is really an **outbox write** in the same DB transaction; the poller (`~1 s`) does the actual `basic.publish`. Diagrams collapse that to one arrow labelled *(via outbox)* unless the timing matters.
- Solid arrow `->>` = request / send. Dashed `-->>` = response / async delivery.
- Error branches use `alt`. Only the interesting failure paths are drawn.

---

## A. Authentication & Sessions
---

### A1. Registration + email verification
---

```mermaid
sequenceDiagram
    autonumber
    actor U as Client
    participant Auth as Auth Service
    participant PG as PostgreSQL
    participant MQ as RabbitMQ
    participant Notif as Notification Service
    participant SMTP

    U->>Auth: POST /auth/register {email, password}
    Auth->>PG: SELECT users WHERE lower(email)=?
    alt email already exists
        Auth-->>U: 409 EMAIL_ALREADY_REGISTERED
    else new
        Auth->>Auth: bcrypt(password, cost 12)
        Auth->>PG: BEGIN
        Auth->>PG: INSERT users (status=PENDING_VERIFICATION)
        Auth->>PG: INSERT email_verification_tokens (ttl 24h, single-use)
        Auth->>PG: INSERT outbox (auth.verification_requested)
        Auth->>PG: COMMIT
        Auth-->>U: 201 {user_id, status: PENDING_VERIFICATION}
    end

    Note over Auth,MQ: outbox poller (~1s)
    Auth->>MQ: publish auth.verification_requested {verification_url, token_expires_at}
    MQ->>Notif: auth.notifications queue
    Notif->>PG: INSERT processed_events (event_id, "notification")
    Notif->>SMTP: send verification email (Qute template)
    SMTP-->>U: inbox: "Verify your email"

    U->>Auth: POST /auth/verify-email {token}
    Auth->>PG: SELECT token, check not expired / not used
    alt invalid or expired
        Auth-->>U: 400 VERIFICATION_TOKEN_INVALID
    else valid
        Auth->>PG: UPDATE users SET status=ACTIVE
        Auth->>PG: mark token used
        Auth-->>U: 200 {status: ACTIVE}
    end
```

**Notes**
- `auth.*` events are **email-only** and bypass the `notification_preferences` matrix ([9 §2](9%20-%20Event%20Catalog.md)).
- Until `ACTIVE`, the user can log in but hits `403 EMAIL_NOT_VERIFIED` on publish / respond / message / review / appearing in search ([4 § Email Registration](4%20-%20Business%20logic.md)).
- Resend: `POST /auth/resend-verification` — 1 / 60 s, ≤ 5 / h; each new token invalidates the previous one.


### A2. Login (with lockout)
---

```mermaid
sequenceDiagram
    autonumber
    actor U as Client
    participant Auth as Auth Service
    participant Redis
    participant PG as PostgreSQL

    U->>Auth: POST /auth/login {email, password}
    Auth->>Redis: GET login_fail:{email}
    alt >= 5 fails in 15-min window
        Auth-->>U: 429 ACCOUNT_LOCKED (Retry-After)
    else not locked
        Auth->>PG: SELECT users WHERE lower(email)=?
        Auth->>Auth: bcrypt.verify(password, hash)
        alt wrong password or unknown email
            Auth->>Redis: INCR login_fail:{email} (EX 900)
            Auth-->>U: 401 INVALID_CREDENTIALS
        else ok
            Auth->>Redis: DEL login_fail:{email}
            alt status = DELETED (within 30-day grace)
                Auth->>PG: UPDATE users SET status=ACTIVE
                Auth->>PG: INSERT outbox (account.reactivated)
            end
            Auth->>Auth: issueAccessToken (JWT 15 min: sub, exp, role, account_status)
            Auth->>PG: INSERT refresh_tokens (family_id, hash, exp +7d)
            Auth-->>U: 200 {access_token, refresh_token, user:{id, role, status}}
        end
    end
```

**Notes**
- Login **succeeds for `SUSPENDED` / `BANNED`** users — they need to reach the status / appeal screen ([8](8%20-%20API%20Specification.md), [4 § Report Workflow](4%20-%20Business%20logic.md)). Authorization for actions is enforced later, per-request, from the `account_status` claim.
- No separate login rate-limit beyond the lockout + the global NFR-Sec-7 policy.


### A3. Per-request authorization (how Core API trusts a JWT)
---

```mermaid
sequenceDiagram
    autonumber
    actor U as Client
    participant Core as Core API
    participant Redis
    participant PG as PostgreSQL

    Note over Core: Auth Service public key (JWKS) is cached in-process, refreshed periodically — NOT fetched per request

    U->>Core: GET /orders/123  (Authorization: Bearer JWT)
    Core->>Core: verify JWT signature locally with Auth public key
    alt bad signature / expired
        Core-->>U: 401 UNAUTHENTICATED
    else signature ok
        Core->>Core: read claims: sub, role, account_status
        Core->>Redis: GET v1:denylist:{sub}
        alt denylist key present
            Core-->>U: 401 SESSION_REVOKED
        else not denylisted
            alt action needs a role the caller lacks / not owner
                Core-->>U: 403 FORBIDDEN
            else authorized
                Core->>PG: SELECT ... (the actual work)
                Core-->>U: 200 OrderResponseDto
            end
        end
    end
```

**Notes — resolves the file 3 ↔ file 4 inconsistency**
- **Local signature verification, no per-request HTTP call to Auth Service.** The only per-request network hop is one `GET` to Redis for the denylist. This matches [4 § Sessions & Tokens](4%20-%20Business%20logic.md); [3 - System Design.md](3%20-%20System%20Design.md) v1.3 was corrected to match (it previously said "validates JWT tokens with Auth Service … every request").
- `v1:denylist:{user_id}` is `SET … EX 900` on ban, suspension, role change, password change, or logout-all. Because the access token lives only 15 min (< 900 s), a denylist entry outlives every token it needs to kill.
- Same verification runs in the Messaging Service before the WebSocket upgrade (A-side of D-flows) and in every service with authenticated endpoints (`SessionDenylistCheck`, [7 § Shared](7%20-%20Application%20Classes.md)).


### A4. Token refresh — rotation + reuse detection
---

```mermaid
sequenceDiagram
    autonumber
    actor U as Client
    participant Auth as Auth Service
    participant PG as PostgreSQL
    participant Redis

    U->>Auth: POST /auth/refresh {refresh_token}
    Auth->>PG: SELECT refresh_tokens WHERE hash = sha256(token)
    alt not found / expired / revoked
        Auth-->>U: 401 REFRESH_TOKEN_INVALID
    else found but already rotated (used_at IS NOT NULL)
        Note over Auth: token theft — someone is replaying an old token
        Auth->>PG: UPDATE refresh_tokens SET revoked=true WHERE family_id = ?
        Auth->>Redis: SET v1:denylist:{user_id} EX 900
        Auth-->>U: 401 REFRESH_TOKEN_REUSED
    else valid + current
        Auth->>PG: BEGIN
        Auth->>PG: UPDATE old token SET used_at=now()
        Auth->>PG: INSERT new refresh_tokens (same family_id, exp +7d)
        Auth->>PG: check original-login age — reject if > 30 days
        Auth->>PG: COMMIT
        Auth->>Auth: issue new access token (15 min)
        Auth-->>U: 200 {access_token, refresh_token}
    end
```

**Notes**
- Sliding window: every refresh grants a fresh 7-day refresh token. Absolute cap: 30 days from the original login, then full re-login regardless of activity.
- Reuse of a rotated token nukes the **whole family** and denylists the user — both the thief and the victim are logged out, victim re-authenticates.


### A5. Google OAuth — first login + role selection
---

```mermaid
sequenceDiagram
    autonumber
    actor U as Client
    participant G as Google
    participant Auth as Auth Service
    participant PG as PostgreSQL

    U->>G: (redirect) consent screen, minimal scopes
    G-->>U: redirect back with ?code&state
    U->>Auth: GET /auth/google/callback?code=&state=
    Auth->>G: exchange code -> id_token (OIDC library)
    Auth->>G: (library) validate id_token signature + claims
    Auth->>PG: SELECT users WHERE google_id = ?
    alt google_id known
        Auth->>Auth: issue access + refresh tokens
        Auth-->>U: 200 LoginResponseDto
    else first time — check email match
        alt existing local account with same verified email
            Auth->>PG: link google_id to that account (strict 1:1)
        else brand new
            Auth->>PG: INSERT users (status=ACTIVE, email pre-verified by Google, role=NULL)
        end
        alt role not set
            Auth-->>U: 200 {role_selection_required: true, temp_token}
            U->>Auth: POST /auth/role {role} (Bearer temp_token)
            Auth->>PG: UPDATE users SET role=? (ROLE_IMMUTABLE if already set)
            Auth-->>U: 200 -> then client calls login/refresh for full tokens
        else role present
            Auth->>Auth: issue access + refresh tokens
            Auth-->>U: 200 LoginResponseDto
        end
    end
```

**Notes**
- Google accounts arrive **email pre-verified** — no `PENDING_VERIFICATION` step.
- Strict 1:1: a Google identity links to exactly one ProFinder account (`GOOGLE_ACCOUNT_ALREADY_LINKED` otherwise).
- Unlink requires a password already set, else the "set password" flow first (A6 variant) — prevents self-lockout.


### A6. Password reset
---

```mermaid
sequenceDiagram
    autonumber
    actor U as Client
    participant Auth as Auth Service
    participant PG as PostgreSQL
    participant MQ as RabbitMQ
    participant Notif as Notification Service
    participant SMTP

    U->>Auth: POST /auth/forgot-password {email}
    Auth->>PG: SELECT users WHERE lower(email)=?
    Auth-->>U: 202 Accepted  (always — anti-enumeration)
    alt account exists
        Auth->>PG: BEGIN
        Auth->>PG: INSERT password_reset_tokens (ttl 15 min, single-use)
        Auth->>PG: INSERT outbox (auth.password_reset_requested)
        Auth->>PG: COMMIT
        Auth->>MQ: publish auth.password_reset_requested (via outbox)
        MQ->>Notif: auth.notifications
        Notif->>SMTP: send reset email {reset_url}
        SMTP-->>U: inbox: "Reset your password"
    else no account
        Note over Auth: nothing sent, but the 202 above already returned
    end

    U->>Auth: POST /auth/reset-password {token, new_password}
    alt token invalid / expired / used
        Auth-->>U: 400 RESET_TOKEN_INVALID
    else valid
        Auth->>PG: BEGIN
        Auth->>PG: UPDATE users SET password_hash = bcrypt(new_password)
        Auth->>PG: mark token used
        Auth->>PG: UPDATE refresh_tokens SET revoked=true WHERE user_id=?
        Auth->>PG: COMMIT
        Auth->>Redis: SET v1:denylist:{user_id} EX 900
        Auth-->>U: 200 OK  (all other sessions now dead)
    end
```


## B. Orders & Responses
---

### B1. Publish order → notify matching professionals → index
---

```mermaid
sequenceDiagram
    autonumber
    actor C as Customer
    participant Core as Core API
    participant PG as PostgreSQL
    participant MQ as RabbitMQ
    participant Idx as Core API · SearchIndexConsumer
    participant ES as Elasticsearch
    participant Notif as Notification Service
    participant FCM
    participant SMTP

    C->>Core: POST /orders/{id}/publish
    Core->>Core: authz (A3) + status must be DRAFT + email verified
    alt not draft
        Core-->>C: 409 ORDER_NOT_DRAFT
    else ok
        Core->>PG: BEGIN
        Core->>PG: UPDATE orders SET status=ACTIVE, published_at, expires_at
        Core->>PG: INSERT outbox (order.published)
        Core->>PG: INSERT outbox (search.order_reindex, reason=published)
        Core->>PG: COMMIT
        Core-->>C: 200 OrderResponseDto (status ACTIVE)
    end

    Note over Core,MQ: outbox poller
    Core->>MQ: publish order.published
    Core->>MQ: publish search.order_reindex

    MQ->>Idx: search.index queue
    Idx->>PG: SELECT order row (source of truth)
    Idx->>ES: upsert order document (guard: version)

    MQ->>Notif: orders.notifications queue
    Notif->>PG: INSERT processed_events (event_id, "notification")
    Notif->>PG: SELECT professionals WHERE category matches AND city matches AND profile ACTIVE
    loop each matching professional (+ the customer)
        Notif->>PG: INSERT notifications row
        Notif->>PG: check notification_preferences (event_type, channel)
        Notif->>FCM: push (if enabled)
        Notif->>SMTP: email (if enabled)
    end
```

**Notes**
- The two outbox rows commit atomically with the status change — a broker outage delays delivery, never drops it.
- `search.order_reindex` and `order.published` are **separate events on separate queues** with separate consumers ([9 §3](9%20-%20Event%20Catalog.md)). The indexer ignores `order.published`; Notification Service never sees `search.*`.
- Matching is exact category + exact city (flat category list, city-only match — [4 § Location and Categories](4%20-%20Business%20logic.md)).
- Consumer is idempotent: a redelivered `order.published` hits the `processed_events` PK and is dropped before any duplicate push.


### B2. Professional submits a response
---

```mermaid
sequenceDiagram
    autonumber
    actor P as Professional
    participant Core as Core API
    participant PG as PostgreSQL
    participant MQ as RabbitMQ
    participant Notif as Notification Service

    P->>Core: POST /orders/{id}/responses {quote_price, message}
    Core->>Core: authz — role PROFESSIONAL, profile_status ACTIVE, email verified
    alt profile INCOMPLETE
        Core-->>P: 409 PROFILE_INCOMPLETE
    else ok
        Core->>PG: BEGIN
        Core->>PG: SELECT order FOR UPDATE — must be ACTIVE and not past expires_at
        alt order CLOSED
            Core->>PG: ROLLBACK
            Core-->>P: 409 ORDER_CLOSED
        else order EXPIRED
            Core->>PG: ROLLBACK
            Core-->>P: 409 ORDER_EXPIRED
        else ACTIVE
            Core->>PG: SELECT responses WHERE order_id=? AND professional_id=?
            alt first submit
                Core->>PG: INSERT responses
                Core->>PG: INSERT outbox (response.created)
            else editing existing
                Core->>PG: UPDATE responses SET quote_price, message, updated_at
                Core->>PG: INSERT outbox (response.edited)
            end
            Core->>PG: COMMIT
            Core-->>P: 201 / 200 ResponseDto
        end
    end

    Core->>MQ: publish response.created OR response.edited (via outbox)
    MQ->>Notif: orders.notifications
    alt response.created
        Notif->>Notif: notify customer — push + email + in-app
    else response.edited
        Notif->>Notif: notify customer — in-app only (D1 decision)
    end
```

**Notes**
- One response per professional per order — the second `POST` is an edit (`201` vs `200`). Sealed bids: the professional never sees other responses or a count.
- The `SELECT … FOR UPDATE` on the order row is what makes "response created just as the customer closes the order" resolve deterministically to one side.


## C. Communication
---

### C1. Chat message over WebSocket (with offline fallback to notification)
---

```mermaid
sequenceDiagram
    autonumber
    actor S as Sender
    participant WS1 as Messaging inst 1
    participant Redis as Redis Pub/Sub
    participant WS2 as Messaging inst 2
    actor R as Recipient
    participant PG as PostgreSQL
    participant Core as Core API
    participant MQ as RabbitMQ
    participant Notif as Notification Service
    participant FCM

    Note over S,WS1: earlier — WS upgrade validated locally + denylist check
    Note over R,WS2: recipient's device SUBSCRIBEd ws:user:{recipient} on its own instance

    S->>WS1: {type: message.send, conversation_id, text}
    WS1->>Core: (HTTP) GET blocks — is sender blocked by / blocking recipient?
    alt blocked
        WS1-->>S: {type: error, code: USER_BLOCKED}
    else allowed
        WS1->>PG: BEGIN
        WS1->>PG: INSERT messages (status SENT)
        WS1->>PG: INSERT outbox (message.sent)
        WS1->>PG: COMMIT
        WS1-->>S: {type: message.status, status: SENT}
        WS1->>Redis: PUBLISH ws:user:{recipient_id} {message.new}
        alt recipient has a live subscription
            Redis-->>WS2: fan-out
            WS2-->>R: {type: message.new, message}
            R->>WS2: {type: message.delivered, message_id}
            WS2->>PG: UPDATE messages SET status=DELIVERED
            WS2->>Redis: PUBLISH ws:user:{sender_id} {message.status: DELIVERED}
            Redis-->>WS1: fan-out
            WS1-->>S: {type: message.status, status: DELIVERED}
        else recipient offline (no subscriber)
            Note over Redis: PUBLISH reaches nobody — that's fine
        end

        Note over WS1,MQ: outbox poller — always runs regardless of online state
        WS1->>MQ: publish message.sent
        MQ->>Notif: messages.notifications
        Notif->>PG: was the message already READ / is recipient active? (heuristic)
        Notif->>PG: INSERT notifications row (in-app)
        Notif->>FCM: push "New message from {sender}" (if enabled + not already seen)
    end
```

**Notes**
- **No LB sticky sessions.** Delivery is `PUBLISH ws:user:{recipient_id}`; Redis fans out to whatever instance(s) hold that subscription. Multi-device = multiple subscribers on the same channel ([8 § Messaging](8%20-%20API%20Specification.md)).
- On reconnect the client calls `GET /conversations/{id}/messages?after={last_seen_id}` to backfill — Pub/Sub has no replay. Same endpoint is the WebSocket-unavailable fallback.
- `message.sent` → Notification Service is the path that reaches an **offline** recipient (push / email / in-app). An online, actively-viewing recipient may get the in-app row suppressed or auto-read.
- `message.deleted` (not drawn): sender calls `DELETE /messages/{id}` → soft-hide + `PUBLISH` a removal to the other party + `message.deleted` event so Notification Service drops any stale unread row. Hard purge after the moderator-review grace period.


## D. Reviews, Search, Files
---

### D1. Post a review → sync moderation → rating recalc → cache bust → reindex
---

```mermaid
sequenceDiagram
    autonumber
    actor C as Customer
    participant Core as Core API
    participant Mod as Moderation Service
    participant PG as PostgreSQL
    participant Redis
    participant MQ as RabbitMQ
    participant Idx as SearchIndexConsumer
    participant ES as Elasticsearch
    participant Notif as Notification Service

    C->>Core: POST /reviews {reviewee_id, rating, text?, photo_file_ids?}
    Core->>Core: authz + email verified
    opt text is present
        Core->>Mod: POST /moderation/check-text {text, type: review} (internal, <200ms)
        Mod-->>Core: ModerationCheckResultDto {approved}
        opt not approved
            Core-->>C: 400 VALIDATION_ERROR (content)
        end
    end
    Core->>PG: BEGIN
    Core->>PG: SELECT review WHERE reviewer_id=? AND reviewee_id=?
    alt exists
        Core->>PG: UPDATE reviews (edit) -> outbox review.edited
    else new
        Core->>PG: INSERT reviews -> outbox review.posted
    end
    Core->>PG: recalc — SELECT AVG(rating), COUNT(*) WHERE professional_id=? AND NOT deleted AND NOT outlier
    Core->>PG: UPDATE professional_profiles SET rating_avg, review_count, version = version + 1
    Core->>PG: INSERT outbox (search.professional_reindex, reason=rating_recalc)
    Core->>PG: COMMIT
    Core->>Redis: DEL v1:profile:pro:{reviewee_id}
    Core-->>C: 201 / 200 ReviewDto

    Core->>MQ: publish review.posted + search.professional_reindex (via outbox)
    MQ->>Idx: search.index
    Idx->>PG: SELECT professional row
    Idx->>ES: upsert professional document (guard: version)
    MQ->>Notif: reviews.notifications
    Notif->>Notif: notify reviewee — "You received a review: N stars"
```

**Notes**
- Rating is **denormalized columns recalculated synchronously in the same transaction**, not a cache ([4 § Professional Rating](4%20-%20Business%20logic.md)). The search sort key `sort_rating` (Bayesian) is derived from those columns.
- Profile cache is **delete-on-write** right after commit; the search result-page cache is just left to expire (60 s TTL, no active bust).
- `review.edited` does **not** re-notify; it still triggers the reindex if the rating moved.
- Moderation sync check is **MVP**; the async ML pipeline is Phase 2 and would add a callback `POST /reviews/{id}/moderation-result`.


### D2. Search request (cache → Elasticsearch)
---

```mermaid
sequenceDiagram
    autonumber
    actor U as Client (public)
    participant Core as Core API · SearchService
    participant Redis
    participant ES as Elasticsearch

    U->>Core: GET /search/professionals?q=&category_id=&city=&rating_min=&sort=recommended&page=1
    Core->>Core: validate params (size <= 100)
    Core->>Core: build cache key v1:search:{normalized params}
    Core->>Redis: GET v1:search:...
    alt cache hit (first ~3 pages only)
        Redis-->>Core: serialized page of cards
        Core-->>U: 200 { items, page, size, total }
    else miss
        Core->>ES: query with filters and scoring
        alt ES unreachable
            ES-->>Core: connection error
            Core-->>U: 503 SEARCH_UNAVAILABLE  (no PostgreSQL fallback)
        else ok
            ES-->>Core: hits (minimal card projection)
            Core->>Redis: SET v1:search:... EX 60
            Core-->>U: 200 { items, page, size, total }
        end
    end
```

**Notes**
- Result cards come **straight from the ES document** — `id, display_name, avatar_thumb_url, rating, review_count, hourly_rate`. Full profile loads separately on click.
- No fallback to PostgreSQL on ES failure — deliberate ([4 § Search Failure Handling](4%20-%20Business%20logic.md)). The nightly full reindex + queued reindex events keep ES eventually correct.


### D3. File upload (avatar / portfolio / evidence / attachment)
---

```mermaid
sequenceDiagram
    autonumber
    actor U as Client
    participant Core as Core API · FileStorageService
    participant MinIO
    participant PG as PostgreSQL

    U->>Core: POST /files (multipart: file, purpose, attached_to_id?)
    Core->>Core: validate size (limits: avatar 5MB, portfolio 15MB, others 10-25MB)
    alt too large
        Core-->>U: 413 FILE_TOO_LARGE
    else
        Core->>Core: sniff magic bytes — real MIME
        alt type not allowed for this purpose
            Core-->>U: 415 INVALID_FILE_TYPE
        else allowed
            opt image
                Core->>Core: strip EXIF and metadata
                Core->>Core: resize (avatar 512x512 + 128x128 thumb, others max 2048px)
                Core->>Core: re-encode JPEG q82
            end
            Core->>MinIO: PUT object(s) under unique key
            alt MinIO down
                MinIO-->>Core: error
                Core-->>U: 503 STORAGE_UNAVAILABLE  (not queued — client retries)
            else stored
                Core->>PG: INSERT files (key, purpose, owner_id, size, mime, thumb_key?)
                Core-->>U: 201 {id, url}
            end
        end
    end
```

**Notes**
- Synchronous, straight through Core API — no presigned-URL round trip, no async processing queue in MVP.
- `GET /files/{id}` → `302` to the MinIO object URL; public for avatar/portfolio, owner/Moderator-gated for review & report evidence.
- Delete removes the object immediately (no delayed GC). No virus scan in MVP.


## E. Moderation & Account lifecycle
---

### E1. Report → moderator decision (ban) → cascade
---

```mermaid
sequenceDiagram
    autonumber
    actor RP as Reporter
    actor M as Moderator
    participant Core as Core API
    participant PG as PostgreSQL
    participant MQ as RabbitMQ
    participant Idx as SearchIndexConsumer
    participant ES as Elasticsearch
    participant Notif as Notification Service

    RP->>Core: POST /reports {reported_user_id, reason, description, evidence?}
    Core->>PG: check — not self, not a duplicate within 24h for this pair
    alt duplicate
        Core-->>RP: 409 DUPLICATE_REPORT
    else ok
        Core->>PG: INSERT reports (status=submitted) + INSERT outbox (report.created)
        Core-->>RP: 201 ReportDto
    end
    Core->>MQ: publish report.created (via outbox)
    MQ->>Notif: reports.notifications
    Notif->>Notif: notify reporter only — "Report received" (moderators are NOT alerted — they poll)

    Note over M,Core: later — moderator works the shared queue
    M->>Core: GET /reports?status=submitted
    M->>Core: POST /reports/{id}/decision {decision: banned, notes}
    Core->>PG: reported user's status must be ACTIVE (not PENDING_VERIFICATION)
    alt target not verified
        Core-->>M: 409 MODERATION_TARGET_NOT_VERIFIED
    else
        Core->>PG: BEGIN
        Core->>PG: UPDATE users SET status=BANNED
        Core->>PG: UPDATE reports SET status=resolved, decision=banned
        Core->>PG: (if customer) for each ACTIVE order: status=CLOSED, closed_reason=account_action
        Core->>PG: INSERT outbox: moderation.banned
        Core->>PG: INSERT outbox: report.resolved
        Core->>PG: INSERT outbox: search.professional_reindex (hide)  — and/or one search.order_reindex per closed order
        Core->>PG: COMMIT
        Core->>Redis: SET v1:denylist:{banned_user_id} EX 900 + revoke refresh tokens
        Core-->>M: 200 ReportDto
    end

    Core->>MQ: publish the batch (via outbox)
    MQ->>Idx: search.index — drop professional doc / drop closed-order docs
    Idx->>ES: delete / update documents
    MQ->>Notif: reports.notifications
    Notif->>Notif: notify banned user (reason + appeal window)
    Notif->>Notif: notify reporter (outcome)
    Notif->>Notif: notify order responders (account closed)
```

**Notes**
- All state changes + every outbox row are **one transaction**. The denylist `SET` + refresh-token revocation happen right after commit so the banned user is logged out within seconds.
- Suspension is the same shape with `status=SUSPENDED` + `suspension_end_date`; a scheduled job (or lazy check at next login) flips it back to `ACTIVE` and emits a reinstatement reindex.
- Ban appeal: `POST /ban-appeals` within 30 days → reviewed by a **different** moderator (`SAME_MODERATOR_REVIEW` guard), one appeal only.


### E2. Account deletion → 30-day grace → finalize
---

```mermaid
sequenceDiagram
    autonumber
    actor U as Client
    participant Core as Core API
    participant Auth as Auth Service
    participant PG as PostgreSQL
    participant Redis
    participant MQ as RabbitMQ
    participant Idx as SearchIndexConsumer
    participant ES as Elasticsearch
    participant Notif as Notification Service

    U->>Core: DELETE /me  (request account deletion)
    Core->>PG: guard — no active ban, no open report against the user
    alt blocked
        Core-->>U: 409 ACCOUNT_DELETION_BLOCKED (resolve open items first)
    else ok
        Core->>PG: BEGIN
        Core->>PG: UPDATE users SET status=DELETED, deleted_at=now(), grace_ends_at=now()+30d
        Core->>PG: scrub PII (name -> "Deleted user", email nulled/hashed, phone/bio cleared)
        Core->>PG: hide professional profile
        Core->>PG: close all ACTIVE orders (account_action)
        Core->>PG: INSERT outbox: account.deletion_requested + search.professional_reindex + search.order_reindex(s)
        Core->>PG: COMMIT
        Core->>Redis: SET v1:denylist:{user_id} EX 900 + revoke refresh tokens
        Core-->>U: 204
    end
    Core->>MQ: publish (via outbox)
    MQ->>Idx: drop documents from ES
    MQ->>Notif: notify user — "30 days to reactivate by logging in"

    alt user logs in within 30 days
        U->>Auth: POST /auth/login
        Auth->>PG: status DELETED + within grace -> UPDATE status=ACTIVE
        Auth->>PG: INSERT outbox (account.reactivated)
        Auth-->>U: 200 tokens
        Note over MQ,Idx: account.reactivated -> re-index profile if it was ACTIVE
    else grace window elapses
        Note over Core: scheduled job scans users WHERE status=DELETED AND grace_ends_at < now()
        Core->>PG: finalize — keep the row (no orphans), data already scrubbed
        Core->>PG: INSERT outbox (account.deletion_finalized)
        Core->>MQ: publish account.deletion_finalized
        MQ->>Idx: hard-purge ES document
        MQ->>Notif: final "account permanently deleted" email
    end
```

**Notes**
- Soft delete — the DB row is **kept** and anonymized so no order/response/review/message is ever orphaned ([4 § Concurrency & Edge Cases](4%20-%20Business%20logic.md)). Reviews the user *wrote* stay, anonymized.
- Reactivation is just "log in during the grace window" — no separate endpoint.
- PII scrub happens **at request time**, not at finalize; finalize is mostly the ES purge + the final notification.


## Changelog
---

| Version | Date | Change |
|---|---|---|
| 1.1 | 2026-09-10 | Standardised to the shared doc format. Account-deletion guard now returns `409 ACCOUNT_DELETION_BLOCKED` (was bare `409 CONFLICT`), matching [8 - API Specification.md](8%20-%20API%20Specification.md). Token-table inserts renamed to `email_verification_tokens` / `password_reset_tokens` to match [6 - Database Schema.md](6%20-%20Database%20Schema.md). |
| 1.0 | 2026-09-07 | Initial: 14 sequence diagrams across Auth (register/verify, login, per-request authz, refresh+rotation, Google OAuth, password reset), Orders (publish, respond), Communication (WebSocket chat), Reviews/Search/Files, Moderation/Account (report→ban cascade, deletion→grace→finalize). Diagram A3 states the resolution of the file 3 ↔ file 4 JWT-validation inconsistency (local verify + Redis denylist); file 3 was corrected to match in v1.3. |
