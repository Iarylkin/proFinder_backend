# ProFinder — Event Catalog

- **Version:** 1.3
- **Date:** 2026-09-10
- **Status:** Stable
- **Purpose:** Every asynchronous domain event on the RabbitMQ bus — routing key, producer, trigger, consumers, payload schema, and idempotency contract. Tier 1 item from [Documentation Roadmap.md](Documentation%20Roadmap.md) (Block B → "Event Catalog + payload schemas").

**Sources reconciled here:**
[3 - System Design.md § Core API → RabbitMQ / § RabbitMQ Queues & Exchanges](3%20-%20System%20Design.md),
[4 - Business logic.md § Events and Notifications / § Reliable Event Publishing (Transactional Outbox)](4%20-%20Business%20logic.md),
[6 - Database Schema.md § 8. Async / Reliability Infra](6%20-%20Database%20Schema.md) (`outbox`, `processed_events`),
[7 - Application Classes.md § 1.6 Outbox / § 4.4 Event Consumption](7%20-%20Application%20Classes.md).

> This file surfaced 5 inconsistencies in file 3's routing-key scheme ([§7](#7-inconsistencies-found--resolution)) — **all now fixed in file 3 v1.2 and file 4 v3.5.** Ten decisions are recorded in [§8](#8-resolved-decisions).

---

## 1. Transport model
---

| Concern | Decision | Source |
|---|---|---|
| Broker | RabbitMQ, single node in MVP (cluster = Phase 2) | file 3 |
| Exchange | one durable topic exchange, `profinder.events` (replaces the built-in `amq.topic` — a named exchange is clearer and can be redeclared by IaC) | [D6](#8-resolved-decisions) (file 3 said `amq.topic`) |
| Publishing | **transactional outbox only** — never a direct inline `channel.publish()` | file 4, file 7 |
| Delivery guarantee | at-least-once → **all consumers must be idempotent** | file 4 |
| Message encoding | UTF-8 JSON, `content_type: application/json` | file 3 |

### Publish path (producers)
---

1. Business service calls `EventPublisher.publish(aggregateType, aggregateId, eventType, payload)` **inside the same `@Transactional` unit** as the business write → one row in `outbox`.
2. `OutboxPollerJob` (`@Scheduled`, ~1 s) reads `outbox WHERE sent = false ORDER BY created_at`, publishes each to `profinder.events` with the routing key = `event_type`, sets `sent = true`, `sent_at = now()`.
3. Broker down → rows accumulate, flush on recovery. The business operation never fails or blocks on the broker.
4. `outbox` rows are **not** deleted by the poller — a separate retention job prunes `sent = true AND sent_at < now() - INTERVAL '7 days'` (keeps a short audit trail; the `7 days` is a `CONFIGURATION.md` knob).

**Auth Service is a second producer.** It owns the `auth.*` events (see [decision D6](#8-resolved-decisions)). Because all services share one PostgreSQL ([file 3](3%20-%20System%20Design.md)), Auth Service gets its **own** `outbox` table rows and its **own** `OutboxPollerJob` instance — same pattern, same table shape, just running inside the Auth Service process. Verification/reset emails then arrive within ~1–2 s of registration (poller interval), which is acceptable for a "check your inbox" screen.

### Consume path
---

1. Consumer receives a message, extracts `event_id`.
2. `INSERT INTO processed_events (event_id, consumer) …` — on conflict (already processed by *this* consumer) → **ack and drop**, do nothing else.
3. Otherwise process, then ack. Processing + the `processed_events` insert should be in one transaction where the consumer owns a database (Notification Service, Core API indexer).
4. Composite PK `(event_id, consumer)` means the same event is legitimately processed once **per** consumer (e.g. Notification Service *and* the search indexer both handle `order.published`).


## 2. Standard event envelope
---

Every message on the bus has this exact top-level shape. `payload` is the only part that varies by event type.

```json
{
  "event_id":       "0192f8c2-... (uuid v7)   — = outbox.id, the idempotency key",
  "event_type":     "order.published          — = the routing key",
  "event_version":  1,
  "occurred_at":    "2026-09-07T12:34:56.789Z — = outbox.created_at, UTC ISO-8601",
  "aggregate_type": "order",
  "aggregate_id":   "0192f8c2-...             — the primary entity this event is about",
  "producer":       "core-api",
  "payload":        { }
}
```

| Field | Notes |
|---|---|
| `event_id` | UUID, equals `outbox.id`. The **only** value consumers dedupe on. |
| `event_type` | Dotted `aggregate.verb`, lowercase, past tense where possible. Equals the routing key. |
| `event_version` | Integer, starts at `1`. Bump **only** on a breaking payload change; add optional fields without bumping. All MVP events are `v1`. Not stored in `outbox` — the publisher stamps it from a constant per event type. |
| `occurred_at` | When the business fact happened (= when the outbox row was written), **not** when it was published or consumed. |
| `aggregate_type` / `aggregate_id` | From `outbox`. Lets a consumer route/log without parsing `event_type`. |
| `producer` | `core-api` \| `messaging-service` \| `auth-service`. |

### Payload conventions
---

- All IDs are UUIDs. All timestamps are UTC ISO-8601. All money is a plain number, USD ([A5](Documentation%20Roadmap.md)).
- Payloads carry **denormalized display fields** the consumer needs (e.g. `professional_name`, `title`) so Notification Service never has to call back into Core API to render a message.
- Payloads carry **IDs, not full entities** — a consumer that needs more does a read, but MVP consumers should not need to.
- Payloads never carry PII beyond what the notification itself shows (no emails, no phone numbers — Notification Service resolves the user's contact channels from its own data + the `notification_preferences` matrix).
- **One deliberate exception:** the `auth.*` events carry the verification / reset link (with its token) in the payload, because the token is the whole point of the email and Notification Service has no other way to get it. These tokens are short-lived and the bus is inside the same trust boundary. `auth.*` events are **email-only** and bypass the `notification_preferences` matrix — a user can't opt out of "verify your email".


## 3. Exchange & queue topology
---

**Exchange:** `profinder.events` (topic, durable)
**Dead-letter exchange:** `profinder.dlx` → one shared `notifications.deadletter` for all notification queues ([D8](#8-resolved-decisions))

| Queue | Bindings (routing keys) | Consumer | DLQ |
|---|---|---|---|
| `orders.notifications` | `order.*`, `response.*` | Notification Service | `notifications.deadletter` |
| `messages.notifications` | `message.*` | Notification Service | `notifications.deadletter` |
| `reviews.notifications` | `review.*` | Notification Service | `notifications.deadletter` |
| `reports.notifications` | `report.*`, `moderation.*`, `account.*`, `user.*` | Notification Service | `notifications.deadletter` |
| `auth.notifications` | `auth.*` | Notification Service | `notifications.deadletter` |
| `search.index` | `search.*` | Core API `SearchIndexConsumer` | **none** — see retry policy |
| `notifications.deadletter` | (dead-lettered messages only) | manual inspection / replay tool | — |

**Phase 2 queues** (not created in MVP — MVP moderation is manual & reactive):
`reviews.moderation`, `messages.moderation`, `users.moderation`, `moderation.deadletter`, and any automated-content-check routing keys.

### Retry & dead-letter policy
---

| Queue class | On consumer failure | Max attempts | Then |
|---|---|---|---|
| `*.notifications` | `nack(requeue=false)` → dead-letter with a retry header; a retry shovel re-queues with backoff | 3 | → `notifications.deadletter`, alert to the ops channel. In-app notification is written **first and independently**, so a failed push/email never loses the in-app record. |
| `*.notifications`, malformed payload (poison) | detect on parse | 0 | straight to `notifications.deadletter`, do not retry |
| `search.index`, Elasticsearch unreachable | `nack(requeue=true)` | unlimited | message stays on the queue; applied on ES recovery. The **nightly full reindex** is the backstop for anything lost. |
| `search.index`, poison payload | detect on parse | 0 | log + ack + drop (a bad reindex event self-heals on the next real profile/order change and the nightly job) |

- Message TTL: **24 h** on the `*.notifications` queues (a day-old notification is stale anyway). **No TTL** on `search.index` — reindex events must survive an ES outage longer than a day ([D7](#8-resolved-decisions)).
- `notifications.deadletter` has **no** TTL — messages sit until a human clears them.


## 4. Event summary
---

Producer is `core-api` unless noted. "Notifies" = the human recipient of the resulting notification (from [file 4 § Events and Notifications](4%20-%20Business%20logic.md)); "—" means no user-facing notification, the event exists only for system consumers.

| # | Routing key | Trigger | Consumers | Notifies |
|---|---|---|---|---|
| 1 | `order.published` | `OrderService.publish()` DRAFT→ACTIVE | Notification Svc; **cascades** `search.order_reindex` | matching professionals; customer |
| 2 | `order.edited` | `OrderService.updateActive()` on an ACTIVE order | Notification Svc; **cascades** `search.order_reindex` | professionals who already responded |
| 3 | `order.expiring_soon` | `@Scheduled` — 1 day before `expires_at` | Notification Svc | customer |
| 4 | `order.expired` | `@Scheduled` — `expires_at` reached | Notification Svc; **cascades** `search.order_reindex` (drop from index; order stays ACTIVE) | customer |
| 5 | `order.extended` | `OrderService.extendExpiration()` | **cascades** `search.order_reindex` (re-add to index) | — |
| 6 | `order.closed` | `OrderService.close()` (manual) **or** `autoCloseForAccountAction()` (ban/suspend/delete) | Notification Svc; **cascades** `search.order_reindex` | responders |
| 7 | `response.created` | `ResponseService.submitOrEdit()` — first submit only | Notification Svc | customer |
| 8 | `response.edited` | `ResponseService.submitOrEdit()` — subsequent edit | Notification Svc | customer — **in-app only** ([D1](#8-resolved-decisions)) |
| 9 | `review.posted` | `ReviewService.submitOrEdit()` — first submit, either direction | Notification Svc; **cascades** `search.professional_reindex` if reviewee is a professional (rating changed) | reviewee |
| 10 | `review.edited` | `ReviewService.submitOrEdit()` — subsequent edit | **cascades** `search.professional_reindex` if rating changed | — (no re-notify) |
| 11 | `review.professional_responded` | `ReviewReplyService.reply()` by the professional | Notification Svc | customer (review author) |
| 12 | `review.customer_replied` | `ReviewReplyService.reply()` by the customer (threaded) | Notification Svc | professional |
| 13 | `message.sent` | `MessageService.send()` — **producer: messaging-service** | Notification Svc (push/email/in-app per prefs; real-time WS delivery is separate, via Redis Pub/Sub) | recipient |
| 14 | `message.deleted` | `MessageService.delete()` — user deletes their own message — **producer: messaging-service** | Notification Svc (remove stale in-app record); Messaging Svc broadcasts the removal to the other party over WS | — |
| 15 | `user.blocked` | `BlockService.block()` | Notification Svc | the blocked user (blocking is not silent) |
| 16 | `report.created` | `ReportService.fileReport()` | Notification Svc | reporter (confirmation only — moderators poll, no alert, [D2](#8-resolved-decisions)) |
| 17 | `report.resolved` | `ReportService.resolve()` — any decision incl. `dismissed` | Notification Svc | reporter (outcome only) |
| 18 | `moderation.warning_issued` | resolve with decision `warning` | Notification Svc | warned user |
| 19 | `moderation.suspended` | resolve with decision `suspended` | Notification Svc; **cascades** `search.professional_reindex` (hide) + `order.closed` per auto-close | suspended user |
| 20 | `moderation.banned` | resolve with decision `banned` | Notification Svc; **cascades** `search.professional_reindex` (hide) + `order.closed` per auto-close | banned user |
| 21 | `moderation.content_removed` | resolve with decision `content_removed` | Notification Svc; **cascades** `search.professional_reindex` if a portfolio photo | affected user |
| 22 | `account.deletion_requested` | `AccountService.requestDeletion()` | Notification Svc; **cascades** `search.professional_reindex` (hide) + `order.closed` per auto-close | the user |
| 23 | `account.reactivated` | login during the 30-day grace window | Notification Svc; **cascades** `search.professional_reindex` (un-hide if was ACTIVE) | the user |
| 24 | `account.deletion_finalized` | `@Scheduled` — grace window elapsed | Notification Svc; search indexer (purge doc) | the user (final "account permanently deleted" email) |
| 25 | `auth.verification_requested` | `AuthService.register()` / resend-verification — **producer: auth-service** | Notification Svc (email only) | the user (verification link) |
| 26 | `auth.password_reset_requested` | `AuthService.forgotPassword()` — **producer: auth-service**, only when the address maps to a real account (endpoint still returns `202` either way — anti-enumeration, [file 8](8%20-%20API%20Specification.md)) | Notification Svc (email only) | the user (reset link) |
| 27 | `auth.password_set_requested` | Google-only account starts "set a password" to unlink — **producer: auth-service** | Notification Svc (email only) | the user (set-password link) |
| 28 | `search.professional_reindex` | cascade from 9/10/19/20/21/22/23/24 and any profile/category/city/rate/experience/availability change | Core API `SearchIndexConsumer` | — |
| 29 | `search.order_reindex` | cascade from 1/2/4/5/6 | Core API `SearchIndexConsumer` | — |


## 5. Payload schemas
---

Notation: `?` = nullable/optional, `[]` = array, `|` = enum of literals. The [envelope](#2-standard-event-envelope) wraps all of these.

### Orders
---

```json
// order.published   aggregate_type: order
{
  "order_id": "uuid", "customer_id": "uuid",
  "category_id": "uuid", "city": "string", "title": "string",
  "budget_min": "number?", "budget_max": "number?",
  "published_at": "iso-8601", "expires_at": "iso-8601"
}

// order.edited   aggregate_type: order
{
  "order_id": "uuid", "customer_id": "uuid",
  "changed_fields": ["budget_min" | "budget_max" | "description" | "title" | "location" | "preferred_date" | "category_id"],
  "responder_ids": ["uuid"],
  "edited_at": "iso-8601"
}

// order.expiring_soon / order.expired   aggregate_type: order
{ "order_id": "uuid", "customer_id": "uuid", "title": "string", "expires_at": "iso-8601" }

// order.extended   aggregate_type: order
{ "order_id": "uuid", "customer_id": "uuid", "new_expires_at": "iso-8601", "extended_at": "iso-8601" }

// order.closed   aggregate_type: order
{
  "order_id": "uuid", "customer_id": "uuid",
  "closed_reason": "manual" | "account_action",
  "responder_ids": ["uuid"],
  "closed_at": "iso-8601"
}
```

### Responses
---

```json
// response.created / response.edited   aggregate_type: response
{
  "response_id": "uuid", "order_id": "uuid",
  "customer_id": "uuid",
  "professional_id": "uuid", "professional_name": "string",
  "quote_amount": "number",
  "created_at": "iso-8601"          // response.edited adds "edited_at": "iso-8601"
}
```

### Reviews
---

```json
// review.posted / review.edited   aggregate_type: review
{
  "review_id": "uuid",
  "reviewer_id": "uuid", "reviewer_role": "CUSTOMER" | "PROFESSIONAL",
  "reviewee_id": "uuid",
  "rating": 1,                      // integer 1..5
  "has_text": true,
  "new_rating_avg": "number", "new_review_count": 0,   // post-recalc, for the reindex cascade
  "created_at": "iso-8601"
}

// review.professional_responded   aggregate_type: review
{ "review_id": "uuid", "reply_id": "uuid", "professional_id": "uuid", "customer_id": "uuid", "created_at": "iso-8601" }

// review.customer_replied   aggregate_type: review
{ "review_id": "uuid", "reply_id": "uuid", "parent_reply_id": "uuid", "customer_id": "uuid", "professional_id": "uuid", "created_at": "iso-8601" }
```

### Messaging  (producer: `messaging-service`)
---

```json
// message.sent   aggregate_type: message
{
  "message_id": "uuid", "conversation_id": "uuid",
  "sender_id": "uuid", "sender_name": "string",
  "recipient_id": "uuid",
  "preview": "string (first ~120 chars, no attachments)",
  "has_attachment": false,
  "sent_at": "iso-8601"
}

// message.deleted   aggregate_type: message
{ "message_id": "uuid", "conversation_id": "uuid", "deleted_by": "uuid", "deleted_at": "iso-8601" }
```

### Blocking
---

```json
// user.blocked   aggregate_type: user
{ "blocker_id": "uuid", "blocked_id": "uuid", "blocked_at": "iso-8601" }
```

### Reports & moderation
---

```json
// report.created   aggregate_type: report
{
  "report_id": "uuid",
  "reporter_id": "uuid",
  "reported_user_id": "uuid",
  "reason": "no_show" | "poor_quality" | "non_payment" | "harassment" | "scam" | "inappropriate_content" | "other",
  "created_at": "iso-8601"
}

// report.resolved   aggregate_type: report
{
  "report_id": "uuid", "reporter_id": "uuid", "reported_user_id": "uuid",
  "decision": "dismissed" | "warning" | "suspended" | "banned" | "content_removed",
  "moderator_id": "uuid",
  "resolved_at": "iso-8601"
}

// moderation.warning_issued   aggregate_type: user
{ "report_id": "uuid", "user_id": "uuid", "moderator_id": "uuid", "warning_text": "string", "prior_warning_count": 0, "issued_at": "iso-8601" }

// moderation.suspended   aggregate_type: user
{ "report_id": "uuid", "user_id": "uuid", "moderator_id": "uuid", "reason": "string", "suspension_end_date": "iso-8601", "suspended_at": "iso-8601" }

// moderation.banned   aggregate_type: user
{ "report_id": "uuid", "user_id": "uuid", "moderator_id": "uuid", "reason": "string", "appeal_window_ends_at": "iso-8601", "banned_at": "iso-8601" }

// moderation.content_removed   aggregate_type: user
{
  "report_id": "uuid", "user_id": "uuid", "moderator_id": "uuid",
  "content_type": "review" | "review_reply" | "message" | "portfolio_photo",
  "content_id": "uuid",
  "removed_at": "iso-8601"
}
```

### Account
---

```json
// account.deletion_requested   aggregate_type: user
{ "user_id": "uuid", "role": "CUSTOMER" | "PROFESSIONAL", "requested_at": "iso-8601", "grace_period_ends_at": "iso-8601" }

// account.reactivated   aggregate_type: user
{ "user_id": "uuid", "reactivated_at": "iso-8601" }

// account.deletion_finalized   aggregate_type: user
{ "user_id": "uuid", "finalized_at": "iso-8601" }
```

### Auth  (producer: `auth-service`, email-only, bypass `notification_preferences`)
---

```json
// auth.verification_requested   aggregate_type: user
{ "user_id": "uuid", "verification_url": "string (absolute link incl. token)", "token_expires_at": "iso-8601", "requested_at": "iso-8601" }

// auth.password_reset_requested   aggregate_type: user
{ "user_id": "uuid", "reset_url": "string (absolute link incl. token)", "token_expires_at": "iso-8601", "requested_at": "iso-8601" }

// auth.password_set_requested   aggregate_type: user
{ "user_id": "uuid", "set_password_url": "string (absolute link incl. token)", "token_expires_at": "iso-8601", "requested_at": "iso-8601" }
```

### Search reindex (system-only)
---

```json
// search.professional_reindex   aggregate_type: professional
{
  "professional_id": "uuid",
  "version": 42,                                  // professional_profiles.version — indexer upserts only if newer
  "reason": "profile_update" | "rating_recalc" | "status_change" | "category_change" | "city_change",
  "occurred_at": "iso-8601"
}

// search.order_reindex   aggregate_type: order
{
  "order_id": "uuid",
  "version": 7,                                   // orders.version
  "reason": "published" | "edited" | "closed" | "expired" | "extended",
  "occurred_at": "iso-8601"
}
```

The reindex consumer fetches the current row from PostgreSQL (source of truth) and upserts the ES document; the payload's `version` is only a staleness guard so an out-of-order redelivery can't overwrite a newer document. Payload deliberately carries **no** document body.


## 6. Cascade map (who emits what)
---

Several business actions emit a **notification event** *and* a **reindex event**, both through the same outbox transaction:

| Business action | Notification event | Reindex event(s) |
|---|---|---|
| Publish order | `order.published` | `search.order_reindex` (add) |
| Edit active order | `order.edited` | `search.order_reindex` |
| Order expires | `order.expired` | `search.order_reindex` (remove) |
| Extend order | — | `search.order_reindex` (re-add) |
| Close order | `order.closed` | `search.order_reindex` (remove) |
| Post/edit review of a professional | `review.posted` / — | `search.professional_reindex` (rating_recalc) |
| Professional edits profile / categories / cities / rate / experience / availability | — | `search.professional_reindex` |
| Suspend / ban / delete a professional | `moderation.*` / `account.*` | `search.professional_reindex` (hide) |
| Suspend / ban / delete a **customer** | `moderation.*` / `account.*` | one `order.closed` **per** their ACTIVE order (`closed_reason: account_action`) → each cascades its own `search.order_reindex` |
| Reactivate account (grace-period login) | `account.reactivated` | `search.professional_reindex` (un-hide, if profile was ACTIVE) |
| Grace window elapses | `account.deletion_finalized` | `search.professional_reindex` (hard-purge the doc) |
| Remove a portfolio photo | `moderation.content_removed` | `search.professional_reindex` |


## 7. Inconsistencies found & resolution
---

Writing this catalog exposed that [3 - System Design.md](3%20-%20System%20Design.md)'s routing-key scheme did not work as written. None were business-logic changes — they were notation fixes. **All applied 2026-09-07 in [file 3](3%20-%20System%20Design.md) v1.2** (and [file 4](4%20-%20Business%20logic.md) v3.5 for the event-name changes).

| # | Problem in file 3 | Fix (applied) |
|---|---|---|
| I1 | Binding patterns are **plural** (`orders.*`, `reviews.*`, `messages.*`) but every event name is **singular** (`order.published`, `review.posted`). A topic `*` matches exactly one word, so `orders.*` would **never** match `order.published`. | Keep event names singular (`order.published`) — they're already used everywhere in file 4. Change file 3's bindings to `order.*`, `review.*`, `message.*`, `report.*`, `moderation.*`, `account.*`, `user.*`. Queue *names* can stay plural (`orders.notifications`). |
| I2 | `search.*` is listed as the pattern for `professional.reindex` / `order.reindex`, which it doesn't match; worse, `order.reindex` would be caught by the orders queue's `order.*` binding and wrongly delivered to Notification Service. | Namespace the reindex events: **`search.professional_reindex`**, **`search.order_reindex`**. Then `search.*` binds them cleanly and nothing leaks into the notification queues. |
| I3 | `user.blocked` is a documented event ([file 4](4%20-%20Business%20logic.md)) with **no** queue binding anywhere in file 3. | Add `user.*` to the `reports.notifications` queue's bindings (it's already the catch-all for `moderation.*` + `account.*`). No new queue. |
| I4 | file 3 shows an `email.queue` bound to `*.email` consumed by a separate **"Email Worker"**, but [file 7 § 4.3](7%20-%20Application%20Classes.md) has Notification Service sending email **inline** in its event consumers (NACK/retry/`notifications.deadletter`). Two different designs. | Drop `email.queue` and the "Email Worker". Notification Service's consumer does in-app + push + email in one handler; email-send failure alone dead-letters via the retry header. Simpler, matches file 7. |
| I5 | file 3 has no `auth.*` routing at all — consistent with "Auth sends its own email", but [D6](#8-resolved-decisions) reversed that: Auth Service now publishes `auth.*` and Notification Service sends the mail. | Add producer **auth-service** and a new queue **`auth.notifications`** bound `auth.*` → Notification Service. Auth Service runs its own `outbox` + poller (shared PostgreSQL). |

**file 4 reconcile (done, v3.5):** removed the `order.draft_created` subsection; renamed `order.response_received` → `response.created` + `response.edited`; added `message.sent` / `message.deleted` / `report.created` / `account.deletion_finalized` / `auth.*` notification descriptions; renamed the reindex events. (Message deletion was already a documented feature in file 4 § History & Retention — no new FR needed, though FR-Messaging could gain an explicit line later.)


## 8. Resolved decisions
---

Resolved 2026-09-07 (D1–D6 by the user; D7–D10 taken as the recommended default — flagged here for visibility, reversible).

| # | Decision | Resolution |
|---|---|---|
| D1 | Low-signal notifications (`order.draft_created`, `response.edited`) | **`order.draft_created` dropped entirely** — no event. **`response.edited` → in-app only** (push + email channels seeded `enabled: false` for this `event_type`). |
| D2 | Notify moderators when a report is filed | **No.** Moderators poll the shared queue as before ([file 4](4%20-%20Business%20logic.md)); `report.created` notifies only the reporter. |
| D3 | Chat message deletion in MVP | **In scope** (and already documented in [file 4 § History & Retention](4%20-%20Business%20logic.md)). `message.deleted` is a real MVP event: user deletes their own message, the other party's client is updated over WS, any stale in-app "new message" notification is cleared. |
| D4 | Auth Service transactional emails | **Routed through Notification Service.** Auth Service becomes a producer of `auth.verification_requested` / `auth.password_reset_requested` / `auth.password_set_requested`; Notification Service owns every email template and the SMTP send. See [I5](#7-inconsistencies-found--resolution). |
| D5 | `account.deletion_finalized` | **Added.** Fires when the 30-day grace window elapses: search indexer hard-purges the ES doc, Notification Service sends a final "permanently deleted" email. |
| D6 | Named exchange | **`profinder.events`** (durable topic), replacing the built-in `amq.topic`. |
| D7 | `search.index` queue TTL | **No TTL** on `search.index` (reindex events pile up through an ES outage; nightly full reindex is the backstop). 24 h TTL stays on the `*.notifications` queues. |
| D8 | Dead-letter layout | **One shared `notifications.deadletter`** for all notification queues in MVP. |
| D9 | `outbox` retention | Prune `sent = true AND sent_at < now() - INTERVAL '7 days'`. `CONFIGURATION.md` knob. |
| D10 | `event_version` | All MVP events are `v1`; the field is stamped by the publisher from a per-type constant, not stored in `outbox`. |


## 9. Changelog
---

| Version | Date | Change |
|---|---|---|
| 1.3 | 2026-09-10 | Standardised to the shared doc format. Renumbered "Resolved decisions" §9 → §8 and "Changelog" §10 → §9 (there was no §8); fixed the `#7-inconsistencies-found--proposed-resolution` anchor links to the real slug. |
| 1.2 | 2026-09-07 | Reconcile edits landed in [file 3](3%20-%20System%20Design.md) v1.2 and [file 4](4%20-%20Business%20logic.md) v3.5 — §7 inconsistencies I1–I5 all fixed at source; §7 / Resolved-decisions updated from "proposed" to "applied". No catalog content change. |
| 1.1 | 2026-09-07 | Applied decisions D1–D6 (user) + D7–D10 (default): dropped `order.draft_created`; `response.edited` in-app only; no moderator alert on `report.created`; `message.deleted` confirmed in MVP; **Auth Service added as a producer** with `auth.*` events + `auth.notifications` queue + its own outbox; `account.deletion_finalized` added. Event count 27 → 29. "Open decisions" replaced by "Resolved decisions"; added I5. |
| 1.0 | 2026-09-07 | Initial catalog: transport model, standard envelope, exchange/queue topology, retry & DLQ policy, 27 events with triggers/consumers/payload schemas, cascade map. Surfaced and proposed fixes for 4 routing-key inconsistencies in file 3 (I1–I4) and 9 open decisions. Written against the corrected scheme (singular `aggregate.verb` routing keys, `search.*_reindex` namespace). |
