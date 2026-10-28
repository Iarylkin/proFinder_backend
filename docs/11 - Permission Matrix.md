# ProFinder — Permission Matrix

- **Version:** 1.3
- **Date:** 2026-10-28
- **Status:** Stable
- **Purpose:** One table of who can do what — roles × actions, plus the account-status and ownership conditions layered on top. [Documentation Roadmap.md](Documentation%20Roadmap.md) Tier 2 ("permission matrix (role × action)").

**Sources:** [2 - Requirements.md § FR-Auth / FR-Moderator / FR-Admin](2%20-%20Requirements.md), [4 - Business logic.md § Authentication & Registration / § Report System](4%20-%20Business%20logic.md), [8 - API Specification.md](8%20-%20API%20Specification.md) (per-endpoint auth column), [6 - Database Schema.md § Enums](6%20-%20Database%20Schema.md).

---

## 1. Roles
---

`users.role` — set once, **immutable in MVP** (support-only change, [FR-Auth-3](2%20-%20Requirements.md)).

| Role | Who | Notes |
|---|---|---|
| `CUSTOMER` | Posts orders, hires, reviews professionals | |
| `PROFESSIONAL` | Responds to orders, has a searchable profile | Has an extra gate: `professional_profiles.profile_status` ∈ {`INCOMPLETE`, `ACTIVE`} |
| `MODERATOR` | Platform staff | Has a sub-level: `users.moderator_level` ∈ {`STANDARD`, `SENIOR`} |

**There is no `ADMIN` role.** "Admin" work = a `MODERATOR` with `moderator_level = SENIOR` ([A3 decision](Documentation%20Roadmap.md), [FR-Admin note](2%20-%20Requirements.md)). A fine-grained per-permission system (FR-Moderator-9) is deferred to a later phase — MVP has exactly the two-tier split in §4.

A `MODERATOR` is **not** also a customer or professional. Moderator accounts don't post orders or have professional profiles. If a staff member needs to use the marketplace they use a separate ordinary account.


## 2. Account-status gate (orthogonal to role)
---

`users.status` restricts every role. Checked per request from the `account_status` JWT claim + the Redis denylist ([10 § A3](10%20-%20Sequence%20Diagrams.md)).

| Status | What the account can do | Error when blocked |
|---|---|---|
| `PENDING_VERIFICATION` | Log in; edit own profile; choose role; read-only browsing (search, view profiles, view orders). **Blocked:** publish order, respond, send messages, post reviews, appear in search. | `403 EMAIL_NOT_VERIFIED` |
| `ACTIVE` | Everything its role allows. | — |
| `SUSPENDED` | Log in (to see status), contact support, read own status page. **Nothing else** — no messaging, no browsing profiles, no orders. A suspended professional is also pulled from search and their responses are hidden. | `403 FORBIDDEN` (`ACCOUNT_SUSPENDED`) |
| `BANNED` | Log in, contact support, file **one** ban appeal (within 30 days). Nothing else. | `403 FORBIDDEN` (`ACCOUNT_BANNED`) |
| `DELETED` | Cannot log in. Logging in within the 30-day grace window silently reactivates → `ACTIVE`. | `401` on login attempt after grace |

> Rows in §3 assume `status = ACTIVE` (and, for professional-only actions, `profile_status = ACTIVE`) unless a cell says otherwise.


## 3. The matrix
---

**Legend:** ✅ allowed · ⛔ not allowed · **own** = only on rows the caller owns · **party** = only if the caller is a participant · ✎ = requires `status = ACTIVE` (not `PENDING_VERIFICATION`) · ★ = `SENIOR` moderator only.

### Orders
---

| Action | Endpoint | CUSTOMER | PROFESSIONAL | MODERATOR |
|---|---|---|---|---|
| Create / edit / delete a **DRAFT** order | `POST/PATCH/DELETE /orders[/{id}]` | ✅ own | ⛔ | ⛔ |
| Publish an order | `PATCH /orders/{id}` (`status: ACTIVE`) | ✅ own ✎ | ⛔ | ⛔ |
| Edit an **ACTIVE** order | `PATCH /orders/{id}` | ✅ own | ⛔ | ⛔ |
| Close / extend an order | `PATCH /orders/{id}` (`status: CLOSED` · `expires_at`) | ✅ own | ⛔ | ⛔ |
| Delete a **problematic** order (any status) | `DELETE /orders/{id}` (moderator path) | ⛔ | ⛔ | ✅ (FR-Moderator-6) |
| View a single order | `GET /orders/{id}` | ✅ | ✅ | ✅ |
| List / browse own orders | `GET /orders?status=` | ✅ own | — | — |
| Browse **ACTIVE** orders to respond to | `GET /orders?category_id=&city=…` | — | ✅ (own categories/cities only) | ✅ |
| Submit / edit a response | `POST /orders/{id}/responses` | ⛔ | ✅ ✎ + `profile_status=ACTIVE` | ⛔ |
| View an order's responses | `GET /orders/{id}/responses` | ✅ own order | ⛔ (sealed bids) | ✅ |

### Reviews
---

| Action | Endpoint | CUSTOMER | PROFESSIONAL | MODERATOR |
|---|---|---|---|---|
| Post / edit a review | `POST /reviews` · `PATCH /reviews/{id}` | ✅ ✎ (of a professional) | ✅ ✎ (of a customer — reliability rating) | ⛔ |
| Delete a review | `DELETE /reviews/{id}` | ✅ own (author) | ✅ own (author) | ✅ (any — FR-Moderator-2) |
| Reply to a review (threaded) | `POST /reviews/{id}/replies` | ✅ party (own review, replying back) | ✅ party (the reviewed professional) | ⛔ |
| View reviews | `GET /users/{id}/reviews` | ✅ public | ✅ public | ✅ public |

*Replies exist only on `CUSTOMER`-direction reviews. A `PROFESSIONAL`-direction (customer reliability) review has no reply in MVP.*

### Profiles, portfolio, categories
---

| Action | Endpoint | CUSTOMER | PROFESSIONAL | MODERATOR |
|---|---|---|---|---|
| View / edit own profile | `GET/PATCH /users/me/profile` | ✅ own | ✅ own | ✅ own (minimal) |
| View a professional's public profile | `GET /professionals/{id}` | ✅ public | ✅ public | ✅ public |
| View a customer's public profile | `GET /customers/{id}` | ✅ | ✅ | ✅ |
| Upload / delete own portfolio photo | `POST/DELETE /users/me/portfolio[/{id}]` | ⛔ | ✅ own | delete: ✅ (any) |
| List categories | `GET /categories` | ✅ public | ✅ public | ✅ public |
| Create / edit / delete a category | `POST/PATCH/DELETE /categories[/{id}]` | ⛔ | ⛔ | ★ `SENIOR` only ([8](8%20-%20API%20Specification.md), FR-Admin-1) |

### Messaging
---

| Action | Endpoint | CUSTOMER | PROFESSIONAL | MODERATOR |
|---|---|---|---|---|
| Open a chat / send a message | `POST /chats/{id}/messages`, WS `message.send` | ✅ ✎ party | ✅ ✎ party | ⛔ (see note) |
| Read chat history | `GET /chats/{id}/messages` | ✅ party | ✅ party | ✅ **any** (disputes — FR-Messaging-6) |
| List own chats | `GET /chats/me` | ✅ own | ✅ own | — |
| Delete a message | `DELETE /messages/{id}` | ✅ own (sender) | ✅ own (sender) | ✅ (content removal — via report decision) |
| Message blocked by a `blocks` row | — | `403 USER_BLOCKED` | `403 USER_BLOCKED` | n/a |

*Moderator "send a message to a user" (FR-Moderator-7, warnings) is delivered as a **notification** from a report decision, not by the moderator joining a customer↔professional chat. A dedicated moderator-messaging surface is not in the MVP chat model.*

### Reports, moderation, blocking
---

| Action | Endpoint | CUSTOMER | PROFESSIONAL | MODERATOR |
|---|---|---|---|---|
| File a report | `POST /reports` | ✅ ✎ (not self, ≤1/day/pair) | ✅ ✎ (same) | ⛔ |
| View the report queue | `GET /reports` | ⛔ | ⛔ | ✅ (shared queue, no assignment) |
| Decide a report (dismiss / warn / suspend / ban / remove content) | `POST /reports/{id}/decision` | ⛔ | ⛔ | ✅ (suspend/ban only if target is `ACTIVE`, not `PENDING_VERIFICATION`) |
| View another user's data for a case | (dashboard) | ⛔ | ⛔ | ✅ (FR-Moderator-5) |
| Ban / unban a user | via report decision / `POST /reports/{id}/decision` | ⛔ | ⛔ | ✅ (FR-Moderator-1) |
| File a ban appeal | `POST /ban-appeals` | ✅ (if `BANNED`, within 30d, once) | ✅ (same) | ⛔ |
| Decide a ban appeal | `POST /ban-appeals/{id}/decision` | ⛔ | ⛔ | ✅ **≠ the moderator who issued the ban** (`SAME_MODERATOR_REVIEW` guard) |
| Block / unblock another user | `POST/DELETE /users/{id}/block` | ✅ | ✅ | ✅ |
| Generate moderation reports / stats | (FR-Moderator-8) | ⛔ | ⛔ | ✅ (Phase-2 detail — analytics endpoint not in MVP) |

### Search, files, notifications, account, auth
---

| Action | Endpoint | CUSTOMER | PROFESSIONAL | MODERATOR |
|---|---|---|---|---|
| Search professionals | `GET /search/professionals` | ✅ public | ✅ public | ✅ public |
| Upload a file | `POST /files` | ✅ ✎ | ✅ ✎ | ✅ |
| View a file | `GET /files/{id}` | avatar/portfolio: public · review/report evidence: **own or MODERATOR** | same | ✅ any |
| Delete a file | `DELETE /files/{id}` | ✅ own | ✅ own | ✅ any |
| Manage own notifications / preferences / device tokens | `/notifications`, `/users/me/notification-preferences`, `/users/me/device-tokens` | ✅ own | ✅ own | ✅ own |
| Delete own account | `DELETE /users/me` | ✅ (blocked during active ban or open report) | ✅ (same) | ✅ (same) |
| Register / login / refresh / logout | `/auth/*` | public / self | public / self | public / self |
| Choose role (once) | `POST /auth/role` | self, if unset (`ROLE_IMMUTABLE` otherwise) | self, if unset | n/a |


## 4. `STANDARD` vs `SENIOR` moderator
---

The only two things gated on `moderator_level = SENIOR` in the MVP:

| Capability | `STANDARD` | `SENIOR` |
|---|---|---|
| Work the report queue: dismiss / warn / suspend / ban / remove content | ✅ | ✅ |
| Decide ban appeals (for bans they didn't issue) | ✅ | ✅ |
| **Create / edit / delete service categories** | ⛔ | ✅ |
| Everything else a moderator can do | ✅ | ✅ |

Everything a hypothetical Admin would do (category management is the concrete case that exists today) is a `SENIOR` capability. When more admin-only actions appear, they attach here rather than spawning a new role. A per-action permission table (FR-Moderator-9) is explicitly **Phase 2**.


## 5. Enforcement
---

- **Role** and **account_status** travel in the JWT access-token claims ([4 § Sessions & Tokens](4%20-%20Business%20logic.md)). No per-request call to Auth Service.
- **`moderator_level`** is **not** a JWT claim — it's read from `users` when a moderator hits a `SENIOR`-gated endpoint (rare, so a DB read is fine; keeps the token small and avoids a token-refresh requirement when a moderator is promoted).
- **`profile_status`** likewise is read from `professional_profiles` on the response-submit / search-visibility paths (it changes rarely and only upward).
- **Ownership** (`customer_id` / `professional_id` / `author_id` / chat participant) is checked against the JWT `sub` in the service layer — see the "Owner" definition in [8 § Endpoints](8%20-%20API%20Specification.md).
- **Instant revocation:** ban / suspend / role change / password change / logout-all set `v1:denylist:{user_id}` (Redis, TTL 900 s) → the next request from any still-valid token gets `401 SESSION_REVOKED`.


## Changelog
---

| Version | Date | Change |
|---|---|---|
| 1.3 | 2026-10-28 | **Paths:** `PATCH /orders/{id}` for publish, close and extend; `GET /chats/me`, `/chats/{id}/messages`; `/users/me/...` for profile, portfolio, preferences, device tokens and account deletion. |
| 1.2 | 2026-10-26 | Added `GET /customers/{id}` (public customer view, any authenticated user, no contact data). |
| 1.1 | 2026-09-10 | Standardised to the shared doc format (metadata, separators). |
| 1.0 | 2026-09-07 | Initial. Role definitions, account-status gate, full role × action matrix across all 5 services, `STANDARD`/`SENIOR` split, enforcement notes. |
