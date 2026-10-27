# ProFinder — Business Logic

- **Version:** 3.6
- **Date:** 2026-09-10
- **Status:** In Progress
- **Purpose:** The authoritative business rules — order lifecycle, reviews, ratings, reports/appeals, auth, profiles, search, messaging, notifications, account deletion, concurrency, caching. All the "why", not the wire format.

---

## Order Lifecycle
---

### Three Core Statuses
---

```mermaid
stateDiagram-v2
    [*] --> DRAFT
    DRAFT --> ACTIVE
    ACTIVE --> CLOSED
    CLOSED --> [*]

    note left of DRAFT
        unpublished, editable, hard-deletable
    end note
    note left of ACTIVE
        published, open for responses
    end note
    note left of CLOSED
        completed, final — no reopening
    end note
```

There is no "delete" action for published orders and no soft-delete/recovery mechanism. A customer's only way to end an order is to **close** it. CLOSED is final — an order can never go back to ACTIVE.


## Status Descriptions
---

### **1. DRAFT** (Draft Order)
---

**When entered:**
- Customer creates a new order but hasn't published it yet
- Order is only visible to the customer who created it

**What happens:**
- Customer can edit all order details (title, description, budget, category, location, etc.)
- Customer can save and leave (draft persists)
- Customer can publish → ACTIVE
- Customer can delete the draft
- Concurrent edits (same customer, two tabs/devices) are guarded by optimistic locking (`version` column, Hibernate `@Version`) — a stale write returns `409 Conflict` and the client refetches

**Timeouts:**
- No time limit — drafts never expire, they persist indefinitely until deleted or published

**Deletion:**
- Hard delete only (no soft delete, no recovery, no grace period)
- No extra restrictions on draft content/attachments beyond the normal limits

**Transitions:**
- `publish()` → ACTIVE
- `delete()` → permanently removed
- A single draft can only be published **once**. Publishing converts it into one ACTIVE order; it cannot be used to spawn multiple ACTIVE orders.

**Data Structure:**
```json
{
  "status": "DRAFT",
  "customer_id": "uuid",
  "title": "string",
  "description": "string",
  "category_id": "uuid",
  "budget_min": "decimal",
  "budget_max": "decimal",
  "location": "string",
  "preferred_date": "date",
  "created_at": "timestamp",
  "updated_at": "timestamp"
}
```


### **2. ACTIVE** (Open for Responses & Communication)
---

**When entered:**
- Customer publishes a draft order → ACTIVE
- Order becomes visible to all professionals in the matching category

**What happens:**
- Each professional submits **one response** with a quote price + message; re-submitting edits that same response (see [Professional Responses](#professional-responses)). No withdrawal.
- Responses are **sealed** — a professional cannot see other professionals' responses, quotes, or the total response count
- Customer sees all incoming responses in real-time (default order: newest first; client can re-sort by price / rating)
- Customer can view professional profiles and ratings
- Customer can open **direct chat with any responding professional** — or with any other professional entirely, chat is not gated by a response (see [Conversation Model](#conversation-model)) — to discuss details, negotiate terms, and coordinate work
- Customer can communicate with **multiple professionals simultaneously** from the same order
- Customer can edit order details anytime (budget, location, description, etc.)
- Editing the order does **not** remove existing responses — they are preserved, and responding professionals are notified of the change
- If a professional submits a response at the moment the customer closes the order, the response creation checks order status in the same transaction and is rejected with `409 ORDER_CLOSED`
- A response to an order whose `expires_at` has passed is rejected with `409 ORDER_EXPIRED` (customer must extend to receive new responses)

**Timeouts / Expiration:**
- Expiration date set by customer (configurable period, e.g., 7 days from publish)
- Customer is notified **1 day before expiration** and again **at the moment of expiration**
- On expiration: order stops appearing in professionals' search/browse results but remains ACTIVE in the customer's own view/history
- Customer can extend the expiration **an unlimited number of times**
- Extension duration is **configurable per extension** (not forced to match the original period)

**Transitions:**
- `closeOrder()` → CLOSED (customer manually closes when work is done or the order is no longer needed)

**Data Structure:**
```json
{
  "status": "ACTIVE",
  "customer_id": "uuid",
  "title": "string",
  "description": "string",
  "category_id": "uuid",
  "budget_min": "decimal",
  "budget_max": "decimal",
  "location": "string",
  "preferred_date": "date",
  "response_count": "integer",
  "responses": [
    {
      "professional_id": "uuid",
      "quote_price": "decimal",
      "message": "string",
      "created_at": "timestamp"
    }
  ],
  "expires_at": "timestamp",
  "published_at": "timestamp",
  "created_at": "timestamp",
  "updated_at": "timestamp"
}
```


### **3. CLOSED** (Order Completed by Customer — Final State)
---

**When entered:**
- Customer manually closes the order (clicks "Close Order" button)
- Indicates: work is done or no longer needed, no more responses accepted

**What happens:**
- Order is removed from the customer's active list, moves to order history
- Professional(s) receive notification: "Order was closed by customer"
- Customer can still view message history with any professional they talked to
- Order is archived permanently

**Timeouts:**
- None — order persists in history indefinitely

**Transitions:**
- **None.** CLOSED is a final state. The order cannot be reopened or moved back to ACTIVE under any circumstances, regardless of how recently it was closed.

**Data Structure:**
```json
{
  "status": "CLOSED",
  "customer_id": "uuid",
  "title": "string",
  "description": "string",
  "category_id": "uuid",
  "budget_min": "decimal",
  "budget_max": "decimal",
  "location": "string",
  "created_at": "timestamp",
  "published_at": "timestamp",
  "closed_at": "timestamp",
  "updated_at": "timestamp"
}
```

> **Note:** Reviews are **not** attached to orders (see [Reviews](#reviews) below) — a CLOSED order has no `reviews` field. A customer can review any professional at any time, independent of whether they ever had an order together.


## State Transitions
---

| From Status | To Status | Triggered By | Condition | Action |
|------------|-----------|--------------|-----------|--------|
| DRAFT | ACTIVE | CUSTOMER | `publish()` called | Order becomes visible to professionals; draft can only be published once |
| DRAFT | (removed) | CUSTOMER | `delete()` called | Permanent hard deletion, no recovery |
| ACTIVE | CLOSED | CUSTOMER | `closeOrder()` called | Order archived, final state, history preserved |
| ACTIVE | CLOSED | SYSTEM | Customer is banned / suspended / deletes account | All the customer's ACTIVE orders auto-close (`closed_at` set, reason `account_action`); responding professionals get the normal `order.closed` notification. Nothing is deleted. |

There are no other transitions. No soft delete, no moderator deletion of orders, no reopening. A professional being banned/suspended does not change order status — that professional is just removed from search and their responses are filtered out of the customer's list until restored.


## Timeouts and Automation
---

| Event | Timeout | Action |
|-------|---------|--------|
| ACTIVE order expires | Configurable (default 7 days) | Customer notified 1 day before and at expiration; order removed from professional search results but stays ACTIVE in customer's history; customer can extend (unlimited times, configurable duration) or close |
| DRAFT persists unused | No limit | Stays indefinitely until published or deleted |
| Review can be posted | Anytime | No time restriction, no dependency on order status |


## Role Permissions
---

### Customer:
---

- ✅ Create order (DRAFT)
- ✅ Edit order details (DRAFT and ACTIVE)
- ✅ Publish order (DRAFT → ACTIVE)
- ✅ Close order (ACTIVE → CLOSED) — final, cannot be undone
- ✅ Hard-delete own DRAFT orders
- ✅ Communicate with multiple professionals via direct messages
- ✅ Post review on any professional, anytime, independent of orders
- ✅ Edit or delete own review anytime
- ✅ Block a professional
- ✅ Report a professional or user (separate from order lifecycle)
- ✅ Soft-delete own account (login disabled, profile hidden, PII scrubbed; 30-day grace, reversible by logging in — see [Account Deletion](#account-deletion))
- ❌ Cannot: reopen a CLOSED order, resolve own reports, moderate content, delete their account while under an active ban or with an open report against them

### Professional:
---

- ✅ View ACTIVE orders in their category (profile must be `ACTIVE`, not `INCOMPLETE`)
- ✅ Submit **one** response per ACTIVE order, and edit it while the order stays ACTIVE
- ✅ Communicate directly with customer via messages
- ✅ Reply to any review (positive or negative)
- ✅ Block a customer
- ✅ Report a customer or user (separate from order lifecycle)
- ✅ Soft-delete own account (same rules as customer — see [Account Deletion](#account-deletion))
- ❌ Cannot: withdraw/retract a submitted response (there is no withdrawal mechanism — a professional simply submits their offer, editable while the order is ACTIVE; the customer decides who to work with and closes the order), see other professionals' responses/quotes, edit other users' reviews, delete or reopen orders, moderate content

### Moderator:
---

- ✅ View all orders (any status) and all messages between users
- ✅ Review and act on user/professional reports (shared queue, no per-moderator assignment)
- ✅ Remove reviews or messages for policy violations (content removed immediately, then the affected user is notified)
- ✅ Issue a **warning** (formal notice, no functional restriction; prior-warning count is shown but there is no automatic escalation)
- ✅ **Suspend** a user for a moderator-chosen number of days (1–365, with 3/7/14/30 presets); the account auto-returns to active when the suspension expires
- ✅ **Ban** a user (permanent by default, appealable within 30 days — the one appealable decision)
- ✅ Review a **ban appeal** — but only for a ban issued by a *different* moderator (or as a senior moderator/admin); one appeal per ban
- ✅ View user history and message data for dispute resolution
- ❌ Cannot: see plaintext passwords, restore a CLOSED order, restore a deleted DRAFT order, review an appeal of their own ban decision

**Note — no separate Admin role:** "Admin" functions (category management, analytics dashboard, system logs — see FR-Admin in file 2) are performed by a Moderator holding the top permission level (FR-Moderator-9), not a 4th role. A dedicated fine-grained permission/role system is deferred to a later phase.


## Events and Notifications
---

> The authoritative event contract (routing keys, payload schemas, consumers, idempotency) is [9 - Event Catalog.md](9%20-%20Event%20Catalog.md). This section describes the **user-facing notification** each event produces; the catalog governs the wire format.

*(Creating a DRAFT order fires no notification — it's an action the customer just took in the UI. There is no `order.draft_created` event.)*

### order.published
---

- **Trigger:** Customer publishes DRAFT → ACTIVE
- **Recipients:** Matching professionals, Customer
- **Message (Professional):** "New order in your category: [Category] - [Budget]"
- **Message (Customer):** "Your order is now live to professionals"

### response.created
---

- **Trigger:** Professional submits their (first) response to an ACTIVE order
- **Recipients:** Customer
- **Message:** "New response from [Professional Name] - Quote: $[price]"

### response.edited
---

- **Trigger:** Professional edits a response they already submitted
- **Recipients:** Customer — **in-app only** (no push, no email; low-signal)
- **Message:** "[Professional Name] updated their response to [Order Title]."

### order.edited
---

- **Trigger:** Customer edits an ACTIVE order (budget, location, description, etc.)
- **Recipients:** All professionals who already responded
- **Message:** "The order [Title] you responded to was updated by the customer."

### order.expiring_soon
---

- **Trigger:** 1 day before `expires_at`
- **Recipients:** Customer
- **Message:** "Your order [Title] expires tomorrow. Extend or close it."

### order.expired
---

- **Trigger:** `expires_at` reached
- **Recipients:** Customer
- **Message:** "Your order [Title] has expired. Extend the deadline or close it."

### order.closed
---

- **Trigger:** Customer manually closes the order
- **Recipients:** Professional(s) who engaged/responded
- **Message:** "Order [Title] was closed by the customer."

### review.posted
---

- **Trigger:** Customer posts a review for a professional (anytime, independent of order status)
- **Recipients:** Professional
- **Message:** "You received a review: [Rating] stars. [Optional: Preview of text]"

### review.professional_responded
---

- **Trigger:** Professional responds to a review
- **Recipients:** Customer
- **Message:** "Professional replied to your review."

### message.sent
---

- **Trigger:** A chat message is sent (published by the Messaging Service)
- **Recipients:** The other party — push / email / in-app per their preferences. Real-time in-conversation delivery goes over the WebSocket separately (Redis Pub/Sub) and is not a notification.
- **Message:** "[Sender Name]: [preview]"

### message.deleted
---

- **Trigger:** A user deletes one of their own messages (in-scope for MVP)
- **Recipients:** No notification. The other party's open conversation is updated over the WebSocket; any not-yet-read "new message" in-app notification pointing at that message is removed.

### user.blocked
---

- **Trigger:** A user blocks another user
- **Recipients:** The blocked user
- **Message:** "[User] has blocked you." (blocking is not silent — the blocked user is informed)

### review.customer_replied
---

- **Trigger:** Customer replies under a professional's response to their review (threaded)
- **Recipients:** Professional

### report.created
---

- **Trigger:** A user files a report
- **Recipients:** Reporter only — "Report received, a moderator will review it." Moderators are **not** notified; they work the shared queue by polling (no assignment, no SLA — see [Report System](#report-system-separate-from-order-lifecycle)).

### moderation.warning_issued / moderation.suspended / moderation.banned / moderation.content_removed
---

- **Trigger:** Moderator decision on a report
- **Recipients:** The affected user (outcome only — warning text / suspension start & end date / ban reason / which content was removed). Not sent when a report is dismissed.

### report.resolved
---

- **Trigger:** Moderator resolves a report
- **Recipients:** Reporter ("Report reviewed. Decision: [outcome]" — outcome only, no detailed reasoning)

### account.deletion_requested
---

- **Trigger:** User requests account deletion
- **Recipients:** The user (confirmation + "you have 30 days to reactivate by logging in")

### account.reactivated
---

- **Trigger:** User logs in during the 30-day grace period
- **Recipients:** The user

### account.deletion_finalized
---

- **Trigger:** The 30-day grace window elapses without a reactivating login (scheduled job)
- **Recipients:** The user — a final "Your account has been permanently deleted" email. The search index document is hard-purged.

### auth.verification_requested / auth.password_reset_requested / auth.password_set_requested
---

- **Trigger:** Registration / resend-verification, forgot-password, or a Google-only account starting a "set a password" flow. Published by the **Auth Service** via its own transactional outbox.
- **Recipients:** The user — **email only**, sent by the Notification Service (which owns every email template). These bypass the `notification_preferences` matrix — a user can't opt out of "verify your email".

### notification delivery model
---

- Every event type is delivered on **push, email, and in-app** by default; each `(user, event_type, channel)` cell can be toggled off in settings (`notification_preferences` matrix). Exceptions: `response.edited` is seeded in-app-only; `message.deleted`, `order.extended` and the `search.*` events produce no notification; `auth.*` and `account.deletion_finalized` are email-only and not user-configurable.
- **In-app:** notification center only, no toast/pop-up.
- **Email:** real-time (no digest in MVP), multipart HTML + plain-text, branded template, per-type unsubscribe + "unsubscribe from all email" footer link. All email (including the Auth Service's `auth.*` transactional mail) is sent by the Notification Service.
- **Push:** Firebase Cloud Messaging; each notification carries a deep-link payload `{type, entity_type, entity_id, url}`.
- **No quiet hours** — delivered at any time of day.


## Report System (Separate from Order Lifecycle)
---

Users can file reports against professionals or customers. Reports are entirely independent of order status — a report does not require a related order.

### Report Types:
---

- **Report Professional:** Customer reports unprofessional behavior, no-show, poor quality, scam
- **Report Customer:** Professional reports non-payment, disrespectful behavior, false claims
- **Report User:** Either party reports for inappropriate content, harassment, etc.

### Submission Rules:
---

- Reporting is **not anonymous** — the reported user's case always shows who filed it
- A user **cannot report themselves**
- A user can report the same person multiple times, but **only once per day** (1 report/day rate limit per reporter–reported pair)
- Evidence attachments: up to ~20 files, same size limits as other file uploads on the platform

### Report Workflow:
---

1. **Report Created**
   - Reporter provides: reason, description, optional evidence (messages, photos)
   - Report goes into a shared moderator queue (no per-moderator assignment — any moderator can pick it up)
   - Reporter receives confirmation

2. **Moderator Review**
   - Moderator reviews report + message history
   - No SLA / response-time requirement — moderators handle reports at their own pace
   - No priority levels — all reports are treated equally, nothing is auto-escalated as "urgent"
   - Moderator can contact either party directly for clarification before deciding

3. **Decision & Action**
   - **Dismiss:** No policy violation found, report closed
   - **Warning:** User receives a formal warning — a notice recorded on the account, no functional restriction. Shown to the user (notification + account-status marker). The moderator sees the count of prior warnings but nothing escalates automatically.
   - **Suspension/ban precondition:** only applies to an email-verified account. A `PENDING_VERIFICATION` account cannot be suspended or banned — the report can still be filed and reviewed, but the moderator can only act on it once the account verifies.
   - **Suspension:** Moderator enters a number of days (1–365, with 3/7/14/30 quick presets). `suspension_end_date = now + N days`. The account auto-returns to active on expiry (scheduled job / lazy check at next login). A suspended professional is removed from search and their responses are filtered out of customers' lists until restored. There is no indefinite suspension — that is what a ban is for.
   - **Ban:** Permanent by default, but **can be appealed** (see [Ban Appeals](#ban-appeals) below)
   - **Content Removal:** Review or message is deleted immediately, and the affected user is notified afterward (not asked for approval beforehand)
   - When a **customer** is suspended or banned, all their ACTIVE orders auto-close (reason `account_action`) and responders are notified
   - While suspended or banned, a user cannot do anything on the platform except log out, contact support, or (if banned) file one ban appeal — no messaging, no viewing profiles, no submitting orders

4. **Notification**
   - Reporter: "Report reviewed. Decision: [outcome]"
   - Reported user: notified of the consequence (warning / suspension start date / ban reason), except when the report is dismissed

### Ban Appeals
---

A permanently banned user can file an appeal against the ban decision. This is the one exception to the "no appeal" simplicity used everywhere else in the report system — warnings, suspensions, and dismissed reports are not appealable, only bans are.

- **Time limit:** the appeal must be filed within **30 days** of the ban (`ban_appeal_window_days`, admin-configurable). After that the ban is final.
- **Reviewer:** a **different** moderator than the one who issued the ban (or a senior moderator / admin). A moderator cannot review an appeal of their own decision.
- **One appeal per ban.** If it is rejected, that is final — there is no appeal of the appeal.
- **Outcome:** upheld → ban lifted, account restored to active (content that was removed stays removed); rejected → ban stands, user notified, no further recourse.
- **No counter-appeal by the reporter.** If a report is dismissed the reporter is simply notified "no action taken"; new evidence means a new report (subject to the 1/day limit).
- **How it is filed:** a banned user sees a single "appeal this ban" screen on login (the only action available to them), or uses the support channel.

### Report Data Structure:
---

```json
{
  "report_id": "uuid",
  "reporter_id": "uuid",
  "reported_user_id": "uuid",
  "order_id": "uuid (optional — reports are not tied to a specific order)",
  "report_type": "professional_conduct" | "customer_conduct" | "user_behavior",
  "reason": "no_show" | "poor_quality" | "non_payment" | "harassment" | "scam" | "inappropriate_content" | "other",
  "description": "string",
  "evidence": [
    {
      "type": "text" | "photo" | "message_link",
      "content": "string or url",
      "timestamp": "timestamp"
    }
  ],
  "status": "submitted" | "under_review" | "resolved",
  "moderator_id": "uuid (nullable)",
  "moderator_notes": "string (nullable)",
  "decision": "dismissed" | "warning" | "suspended" | "banned" | "content_removed" | null,
  "action_taken": "string (nullable)",
  "suspension_end_date": "timestamp (nullable)",
  "created_at": "timestamp",
  "resolved_at": "timestamp (nullable)"
}
```


## Search and Visibility
---

**Professionals see:**
- ACTIVE orders only, in their selected categories, matching their city
- Not visible: CLOSED orders, expired orders

**Customers see:**
- Their own DRAFT, ACTIVE, and CLOSED orders (full history, nothing is ever hidden by soft-delete)


## Order Data Structure (Complete)
---

```json
{
  "id": "uuid",
  "status": "DRAFT | ACTIVE | CLOSED",
  "customer_id": "uuid",

  "title": "string",
  "description": "string",
  "category_id": "uuid",
  "budget_min": "decimal",
  "budget_max": "decimal",
  "location": "string",
  "preferred_date": "date (optional)",

  "responses": [
    {
      "professional_id": "uuid",
      "quote_price": "decimal",
      "message": "string",
      "created_at": "timestamp"
    }
  ],
  "response_count": "integer",

  "created_at": "timestamp",
  "published_at": "timestamp (nullable)",
  "closed_at": "timestamp (nullable)",
  "expires_at": "timestamp (nullable)",

  "version": "integer (optimistic locking)",
  "updated_at": "timestamp"
}
```


## Decisions Made (Orders)
---

| Question | Decision |
|----------|----------|
| Can professional submit multiple responses to same order? | ❌ No — exactly one response per professional per order, editable while the order is ACTIVE (re-submitting replaces it) |
| Can customer edit ACTIVE order details? | ✅ Yes — existing responses are kept, professionals notified |
| Can customer communicate with multiple professionals? | ✅ Yes |
| Can professional withdraw a response? | ❌ No — there is no withdrawal mechanism at all (they can edit it instead) |
| Can professionals see each other's responses / quotes? | ❌ No — sealed bids |
| Can a CLOSED order be reopened? | ❌ No, never — final state |
| Is there a soft-delete / DELETED status for orders? | ❌ No — customer can only close an order or hard-delete a DRAFT |
| Maximum responses per order? | No artificial cap — naturally bounded by the number of professionals in the category + city (one each) |
| Are reviews tied to a specific order? | ❌ No — fully decoupled, see [Reviews](#reviews) |
| Can chat start without an order response? | ✅ Yes — a customer can message any professional at any time; chat is not gated by a response (see [Conversation Model](#conversation-model)) |
| Are categories hierarchical? | ❌ No — flat list, no parent/leaf tree (see [Service Categories](#service-categories)) |
| `preferred professional level` field on orders? | ❌ Dropped — leftover from the removed professional-verification concept, never had a defined meaning |
| `latitude`/`longitude` on orders? | ❌ Dropped — search is city-only (no radius), so these fields served no purpose |
| Order/quote currency? | ✅ Single platform currency: **USD** |
| Timezone handling? | No per-user timezone support in MVP — plain timestamps, no offset/TZ conversion; can be added later if needed |


## Rating and Reputation System
---

The platform intentionally uses a **single metric**: a 1–5 star rating. There are no secondary metrics (response rate, completion rate, etc.) for either role.

### Professional Rating
---

- **Calculation:** Simple arithmetic mean of all (non-deleted, non-outlier) reviews
- **Storage:** kept as denormalized columns `rating_avg` + `review_count` on the professional row, **recalculated synchronously in the same transaction** whenever a review is created / edited / deleted (`AVG(rating), COUNT(*) WHERE professional_id=? AND NOT deleted AND NOT outlier`). It is **not** a separate Redis cache. These fields flow into the profile cache and the Elasticsearch document.
- **Outlier filtering:** Extreme ratings that look like brigading (e.g. a burst of all-5★ or all-1★ ratings) are filtered out before the average is computed
- **Display:** 0 reviews → shown as 0; 1 review → the average is just that review's rating; no minimum-review threshold to start displaying a rating. The **displayed** value is always this raw mean.
- **Search sorting uses a Bayesian-adjusted value:** `sort_rating = (C·m + Σratings) / (C + review_count)`, where `m` ≈ platform-wide mean rating and `C` ≈ 5 (both admin-configurable). This is used **only as a sort key** (so a 0-review professional is not buried at literal 0) — it is never shown to users.
- **Breakdown:** Overall rating only — no per-criteria breakdown (e.g. no separate "communication" / "quality" scores)
- **Deleted reviews:** When a review is deleted, the rating **is recalculated** without it (deleted reviews do affect/change the rating)
- **Low rating does not hide a professional:** professionals with under 3★ can still appear in search results — no automatic filtering

### Customer Reliability Rating
---

- **Calculation:** Same model — simple arithmetic mean (with outlier filtering) of all reviews left by professionals about the customer
- **Submission mechanics:** mirrors the customer→professional review model — a professional can leave **one review per customer** (1–5 stars + optional text), postable at any time, independent of any order. Re-submitting edits the existing review rather than creating a duplicate.
- **No reply in MVP:** unlike professional replies to customer reviews (which are threaded), the customer **cannot reply** to a professional's review of them. May be added in a later phase.
- **Display:** Shows 0 if no reviews yet


## Professional Responses
---

### Submitting a Response
---

- Professional can respond to any ACTIVE order in their category, provided their own profile is `ACTIVE` (not `INCOMPLETE`)
- Response includes: quote price + message with their proposal/offer
- **One response per professional per order.** Submitting again edits the existing response (mirrors "one review per professional"). It is freely editable while the order stays ACTIVE; the customer sees an "(edited)" marker + `updated_at`. No edit history is kept.
- There is **no withdrawal** — once submitted, a response stands (or is edited). The customer chooses who to engage with and closes the order when done; there is no formal "decline"/"reject" action on the professional's side either.
- **Sealed bids:** a professional never sees other professionals' responses, quotes, or the response count.
- Rejected: a response to a `CLOSED` order (`409 ORDER_CLOSED`) or an expired order (`409 ORDER_EXPIRED`).

### Customer's View of Responses
---

- The customer sees **all** responses, no algorithmic hiding. Default order: newest first. Client may re-sort by quote price or professional rating.
- Paginated at 20 per page.
- Responses from a professional who has since been banned / suspended / deleted their account are filtered out (restored if the professional is reinstated).


## Location and Categories
---

### Location Management
---

- Professional provides: city (single text field)
- Professional can work in **multiple cities**, with no limit on how many
- Customer specifies: city/location for the order
- Search matches professionals **by city only** — there is no radius/"as the crow flies" distance calculation

### Service Categories
---

- **Flat list — no hierarchy.** Categories are a single-level list (e.g. Plumbing, Electrical, Painting, Cleaning), not a parent/child tree. An order's `category_id` matches a professional only if the professional has that exact category — no parent-vs-leaf matching logic needed.
- Professional can select an **unlimited number of categories**
- Categories are predefined by admin

### Currency
---

- Single platform currency: **USD**. All monetary fields (`hourly_rate`, `quote_price`, `budget_min`, `budget_max`) are USD — no multi-currency support in MVP.


## Authentication & Registration
---

### Email Registration
---

- **Email validation:** Jakarta Bean Validation `@Email` + length ≤ 254; normalized on save (trim + lowercase); case-insensitive `UNIQUE` index. No MX-record check. Disposable-domain blocklist is an admin-configurable list (empty in MVP). No domain whitelists.
- **Email verification link is valid for 24 hours** (`email_verification_ttl`, admin-configurable).
- **Password-reset ("forgot password") link is valid for 15 minutes** (`password_reset_ttl`, admin-configurable — kept short because it is more sensitive).
- **Before verification:** the account is created in status `PENDING_VERIFICATION`. The user *can* log in, but until the email is confirmed these are blocked (`403 EMAIL_NOT_VERIFIED`): publishing an order, responding to an order, sending chat messages, posting reviews, appearing in search (for professionals). Allowed: editing own profile, choosing role, read-only browsing.
- **Resend verification:** rate-limited to 1 / 60 s and ≤ 5 / hour per account. Each new link invalidates the previous one (exactly one active token at a time); tokens are single-use. No hard retry cap that locks the account.
- **Account lockout:** 5 failed logins in a sliding 15-minute window (Redis, key `login_fail:{email}`) → login temporarily blocked for 15 minutes (`429` + `Retry-After`). Always temporary, a successful login resets the counter. No "suspicious login" email in MVP.
- User can change their account email later; the change is authorized via the currently-verified email, and the account's verified status is preserved (no fresh full re-verification cycle is forced).
- No dedicated extra rate limit specifically for the login endpoint beyond the general policy (NFR-Sec-7) plus the lockout rule above.

### Google OAuth
---

- Requested scopes are kept minimal (just what's needed to identify the user — no broad profile/calendar access)
- Edge cases not covered by our own logic (e.g. a Google account without an email, or a deleted Google account) are handled the standard way the Google OAuth/OIDC library handles them — no custom handling needed
- A ProFinder account can be linked to **exactly one** Google account (strict 1:1 link), not multiple
- **Unlinking Google:** allowed if the account already has a password. If the account is Google-only (no password), the user must first complete a "set password" flow (link sent to the verified email), then unlink — this prevents self-lockout.
- **Role selection:** after the first Google login, if no role is set yet, the user is sent to a mandatory "Customer or Professional?" screen, then profile completion. The role is never auto-detected.
- **Role is immutable in MVP** (for both email and Google users) — changing it is a support-only operation.

### 2FA — NOT in MVP
---

2FA was removed from MVP scope (decision 2026-08-27). The architecture leaves room for TOTP, but there is no 2FA enrolment, enforcement, or recovery flow in the MVP. (This supersedes NFR-Sec-3.)

### Password Policy
---

- Minimum length: **8 characters by default**, admin-configurable (`password_min_length`)
- Complexity: all standard checks (uppercase, lowercase, numbers, special characters) enabled by default, admin-configurable
- Expiration: passwords **never expire** — only the user themselves can trigger a change
- History: password **reuse is allowed**, no restriction on reusing old passwords
- Hashing: bcrypt, cost factor 12 (NFR-Sec-1) — plaintext is never stored or visible to anyone
- Changing the password (or an admin ban / role change) adds the user to the Redis session denylist (`SET v1:denylist:{user_id} EX 900`) and kills their refresh tokens → all existing sessions are forced to re-authenticate

### Sessions & Tokens
---

- **Access token:** JWT, 15-minute absolute lifetime. Carries `sub`, `exp`, **role**, and **account_status** as claims. Core API verifies the signature locally with the Auth Service public key (no network call) and reads role/status from the claims.
- **Per-request revocation check:** one `GET v1:denylist:{user_id}` in Redis. If the key exists → `401`, forced re-login. The key is set (TTL 900 s) on ban, suspension, role change, password change, or "log out everywhere".
- **Refresh token:** 7 days, **sliding** — each use issues a new refresh token with a fresh 7-day window (rotation). Reuse of an already-rotated token → the whole token family is revoked (theft detection). Absolute cap: no session survives past **30 days** from the original login without a full re-login.
- **Storage:** the refresh token lives in client storage / the request body — **no cookies anywhere**, which is why CSRF protection is not needed.
- Simultaneous sessions from multiple devices are allowed — logging in elsewhere does not kick out an existing session.


## Profile Management
---

### Customer Profile
---

- **Required:** only the registration fields — verified email, display name / nickname, role. **Everything else is optional forever and gates nothing** — avatar, phone, city, bio, preferred categories. Publishing an order does not require a complete profile.
- `preferred categories` is a frontend personalisation hint only — no backend logic depends on it.
- Phone number: country selector + country code, validated against the national number format
- Avatar: exactly one photo; max upload size is admin-configurable
- Bio: 2000 characters by default, admin-configurable
- No "preview my profile as others see it" mode — not needed
- Profile editing is not throttled — customer can edit anytime, as often as they like

### Professional Profile
---

- **Two states:**
  - `INCOMPLETE` — the professional can log in and edit their profile, but does **not** appear in search and **cannot** submit responses.
  - `ACTIVE` — reached as soon as the required fields are set: **display name / nickname + at least 1 service category + at least 1 city**. `ACTIVE` is sticky — once reached, the profile never reverts to `INCOMPLETE`: editing cannot remove the last remaining category or city, only **replace** it (swap), so the ≥1/≥1 invariant always holds.
- Hourly rate and years of experience are **optional** for reaching `ACTIVE`, but a professional who leaves them blank will not be matched by the price / experience filters.
- bio, avatar, public contact fields, portfolio — optional, and their absence does **not** hurt search ranking.
- Years of experience: validated against a plausible age-based bound (can't claim more working years than realistically possible for their stated age)
- Hourly rate: from **0 to unlimited**, no upper cap
- Service categories: unlimited number
- Cities: unlimited number, no format restriction
- Working hours: a **single fixed schedule** — no per-day-of-week variation (can't set different hours for Monday vs Tuesday)
- Availability status: a manual toggle the professional switches themselves; it directly affects visibility in search (independent of working hours)


## Reviews
---

Reviews are a **standalone entity, fully decoupled from orders**. A customer can leave a review for any professional at any time — the professional does not need to have responded to an order, and there does not need to be any order relationship between them at all.

### Posting
---

- Rating (1–5 stars) is required; text is optional
- Photos can be attached
- Reviews are published **immediately** — there is no pre-publish moderation queue. Moderation is reactive: content only gets reviewed by a moderator if someone reports it (see [Moderation](#moderation--reports)); an automated moderation system is planned for later, not MVP
- A customer effectively has **one review per professional** — trying to post again just edits/replaces the existing review instead of creating a duplicate

### Editing & Deletion
---

- Customer can edit their own review at any time, even months later
- Editing does not force a moderation re-check (auto-recheck is deferred to when automated moderation is built) — it only gets scrutinized if reported
- Customer can delete their own review; deleting it recalculates the professional's rating
- Moderator can remove a review without a customer request, for policy violations — content is removed immediately and the author is notified afterward

### Professional Responses to Reviews
---

- Professional can respond to **any** review, not just negative ones
- The professional's response goes through the same (reactive) moderation as reviews
- The thread is **not one-shot** — the customer can reply back to the professional's response (a small threaded conversation under the review), and the character limit for these replies matches the main review's limit


## Search & Discovery
---

### Filters
---

- **Location:** matched by city only — no distance/radius search, no "as the crow flies" calculation
- **Category:** exact match against the flat category list (no parent/leaf — categories are not hierarchical, see [Service Categories](#service-categories))
- **Rating:** slider from 0.0 to 5.0, in **0.1 increments**
- **Price:** shown as a range (bounded by the actual min/max hourly rates in the data), not free-typed min/max fields
- **Availability:** a simple boolean — is the professional's profile currently marked "available" — not tied to working-hours schedule
- **Experience:** exact number of years entered by the customer, not a bucketed range

### Sorting
---

- **Default ("Recommended"):** available professionals first → `sort_rating` (Bayesian, see [Rating](#professional-rating)) desc → `review_count` desc → most recently active.
- **"Relevance" (only when there is a text query):** Elasticsearch `multi_match` over name + bio + category names (BM25), wrapped in `function_score` that multiplies by `ln(1 + sort_rating)` and boosts `available`. All filters (city, category, price, experience, rating slider) are hard `filter` clauses — they do not affect the score.
- **Explicit sort options:** `rating` (raw mean desc), `price` (hourly_rate asc), `newest` (created_at desc), `popularity` (`raw_mean × log10(review_count + 1)` desc).
- There is no "response time" sort/metric — removed entirely (no messaging SLA exists).
- Saved search preferences are **not persisted on the backend** — if this is offered at all, it's a frontend-only (local) concern.

### Results Display
---

- Pagination: **20 results per page by default**, admin-configurable (`page_size_default`, `page_size_max` = 100)
- **Result card (minimal projection served straight from Elasticsearch):** `id`, `display_name`, `avatar_thumb_url`, `rating` (1 decimal) + `review_count`, `hourly_rate`. `city` and `available` are trivially includable if the frontend wants them. Everything else (full bio, categories, experience, portfolio, reviews, contacts, working hours) loads when the profile is opened.
- There is no "featured" professional concept and no pay-to-rank/promoted listings in the current scope

### Search Failure Handling
---

- If Elasticsearch is unavailable, the API returns an **error to the client** (`503 SEARCH_UNAVAILABLE`) — there is no fallback to querying PostgreSQL directly

### Elasticsearch Sync
---

Hybrid, event-driven + reconcile:
1. On any change to a professional's profile / categories / cities / rate / experience / availability toggle **and** on a rating recalculation, Core API publishes `search.professional_reindex` (and `search.order_reindex` for order changes) to RabbitMQ — via the transactional outbox (see [Concurrency & Edge Cases](#concurrency--edge-cases)), never as a synchronous inline write.
2. A lightweight consumer (a module inside Core API for MVP) upserts the Elasticsearch document, idempotent on `version` / `updated_at`.
3. A **nightly full reindex** over all `ACTIVE` professionals reconciles any lost events / drift.
4. If Elasticsearch is down when an event fires, the message stays on the `search.index` queue (unlimited retry, no TTL, no dead-letter) and is applied on recovery; the nightly reindex is the backstop. PostgreSQL is the source of truth; ES lag is acceptable.

### Search Result Caching
---

- Key = hash of the normalized query params: `search:v1:{category}:{city}:{rating_min}:{price_min}:{price_max}:{exp}:{available}:{sort}:{page}:{size}`.
- Value = the serialized page of result cards.
- **TTL = 60 s**, no active invalidation — it just expires. Only the first ~3 pages are cached.


## Messaging & Communication
---

### Conversation Model
---

- A customer can open a chat with **any** professional at any time — from a search result / professional profile, or after that professional has responded to one of the customer's orders. Chat is **not gated** by an order response (resolves the earlier contradiction between FR-Communication-1 and UC-7 — UC-7's "can initiate chat... or contact directly" is the one that holds).
- A conversation is keyed by the **(customer_id, professional_id) pair**, not by a specific order — mirrors how reviews are decoupled from orders. Later interactions on a different order continue the same conversation rather than starting a new thread.

### Message Content
---

- Text messages: roughly **200 characters** max per message
- File sharing (a **later-phase** feature, not MVP-first): image ≤ 10 MB, any other file ≤ 25 MB (both admin-configurable). All file types **except executables** (`.exe .bat .cmd .sh .msi .scr .com .jar` and their MIME types are blocked); attachments are always served with `Content-Disposition: attachment`, never an executable content-type.
- Photos are auto-compressed on upload (re-encoded, max 2048 px long edge, JPEG quality 82, EXIF/metadata stripped, original format kept); no separate thumbnail — the compressed version is served directly.
- Voice/video is architecturally supported (WebSocket-based) but not implemented until a later iteration.

### History & Retention
---

- Messages are retained **forever**, until a user deletes them
- When a user deletes a message, it disappears completely from the UI for both parties (no "[deleted]" placeholder)
- Deleted messages are kept in the database for a short grace period (a couple of days) so a moderator can review them for disputes, then permanently purged
- No full-text search over message history — not needed

### Real-Time Delivery
---

- WebSocket is the default transport
- Delivery status is tracked through three states: **Sent → Delivered → Read**
- Typing indicators are **not implemented in MVP** — cut for simplicity, can be revisited later

### Blocking
---

- Either party (customer or professional) can block the other
- Blocking does **not** hide the historical chat — both sides can still scroll back and read old messages, only new messages are prevented
- Blocking is **not silent** — the blocked user is informed that they've been blocked


## Notifications
---

### Delivery Rules
---

- **Preferences model:** a matrix `notification_preferences(user_id, event_type, channel, enabled)` with `channel ∈ {push, email, in_app}`. Every cell defaults to enabled; the user toggles any cell in settings.
- **Push:** Firebase Cloud Messaging (FCM) directly. Device tokens live in `device_tokens(user_id, token, platform, created_at, last_seen_at)`, upserted on login and on the SDK's `onNewToken` callback. A token is deleted when FCM returns `UNREGISTERED`/`INVALID`, on logout of that device, or after 60 days of inactivity. Every notification carries a deep-link payload `{type, entity_type, entity_id, url}`.
- **In-app:** stored in PostgreSQL (`notifications(id, user_id, type, entity_type, entity_id, title, body, is_read, created_at, read_at)`). Notification center only — no toast/pop-up. Paginated 20 per page, newest first. Badge = count of unread (index on `(user_id, is_read)`). Mark-as-read and mark-all-read are persisted. A background job purges **read** notifications older than `inapp_retention_days` (default 90, configurable); unread are kept indefinitely.
- **Email:** real-time per event (no digest in MVP — FR-Notif-6 is Phase 2). `multipart/alternative` (HTML + plain-text) in every email, branded template (Qute). Unsubscribe at two levels: per-event-type toggles, and an "unsubscribe from all email" link in every footer.
- **There are no "quiet hours."** Notifications are delivered at any time of day; this feature was considered and explicitly rejected.
- **Not implemented in MVP:** notification grouping (e.g. "5 new responses to order X"), and per-event-type notification sound customization — both cut, can be revisited later.


## Moderation & Reports
---

MVP moderation for user-generated content (reviews, review replies, portfolio photos, messages) is **manual and reactive only**: content is published immediately upon creation, and a moderator only looks at it if a user files a report. Proactive automated checks (spam/profanity filters, ML toxicity scoring, image classification) described in the System Design are a planned future phase, not part of the current MVP behavior.

See [Report System](#report-system-separate-from-order-lifecycle) above for the full report workflow, and [Portfolio Management](#portfolio-management) below for portfolio-specific rules.


## Portfolio Management
---

- A professional can upload an **unlimited number of portfolio photos** (≤ 15 MB each). The profile **avatar** is limited to exactly one photo (≤ 5 MB).
- Photos are compressed on upload: re-encoded, EXIF/metadata stripped, original format kept, JPEG quality 82. Avatar → max 512×512 (center-cropped to square) **plus** a 128×128 `avatar_thumb` (used by search cards). Portfolio → max 2048 px on the long edge, single compressed version served (no extra thumbnail).
- File type is validated by magic bytes; only image types are accepted (`image/jpeg`, `image/png`, `image/webp`, `image/heic`/`heif`, `image/gif`).
- Photo dimensions/aspect ratio are not otherwise constrained.
- Captions are optional, up to 2000 characters.
- Portfolio photos are standalone — they are **not** assigned to a specific service category.
- Ordering: newest photos are shown first.
- Visibility: portfolio is visible to **everyone**, not gated behind being a registered/logged-in customer.
- No watermarking is applied to portfolio images.
- Customers **can download** full-resolution portfolio images.
- Moderation is reactive only (see [Moderation & Reports](#moderation--reports)): photos go live immediately; a moderator reviews and can reject a specific photo only if it's reported, giving feedback at that point. There is no appeal process for a rejected photo.


## Concurrency & Edge Cases
---

- **Optimistic locking:** mutable entities (order, profile) carry a `version` column (Hibernate `@Version`). `UPDATE ... WHERE id=? AND version=?` affecting 0 rows → `409 Conflict`, the client refetches and retries. No last-write-wins.
- **In-flight response vs. order close:** response creation checks `WHERE status='ACTIVE'` (or `SELECT ... FOR UPDATE` on the order row) in the same transaction. If the order is already `CLOSED` → `409 ORDER_CLOSED`.
- **Party banned/suspended with an ACTIVE order:** customer → all their ACTIVE orders auto-close (reason `account_action`), responders notified; professional → removed from search, their responses filtered out of customers' lists, restored on reinstatement. Nothing is deleted.
- **No orphaned data:** account deletion is a *soft* delete — the DB row is kept and only anonymized (see [Account Deletion](#account-deletion)), so a deleted order with a dangling response / a deleted professional with a dangling review / orphaned messages simply cannot happen.

### Reliable Event Publishing (Transactional Outbox)
---

- Events are written to an `outbox` table **in the same database transaction** as the business change.
- A background poller (`@Scheduled`, ~1 s) reads `outbox WHERE sent = false`, publishes to RabbitMQ, marks `sent = true`.
- If RabbitMQ is down, rows accumulate and flush on recovery — **zero loss**. The business operation never fails because of a broker hiccup.
- Consumers are **idempotent** (RabbitMQ is at-least-once): dedup by `event_id` (`processed_events` table, composite PK `(event_id, consumer)`); Elasticsearch upserts by `version`.
- **Three services publish:** Core API (most events), Messaging Service (`message.*`), and Auth Service (`auth.*` transactional emails) — each runs its own outbox + poller against the shared PostgreSQL.
- Full contract — routing keys, envelope, per-event payload schemas, queue topology, retry/DLQ policy: [9 - Event Catalog.md](9%20-%20Event%20Catalog.md).

### System-Failure Behaviour
---

| Failure | Behaviour |
|---|---|
| RabbitMQ down | Events queue in the outbox, flush on recovery |
| Elasticsearch down | `503 SEARCH_UNAVAILABLE` to the client, no PostgreSQL fallback; reindex events wait in the queue |
| Email (SMTP) down | Notification Service NACKs → RabbitMQ redelivers; max 3 retries → `notifications.deadletter` for manual review. The in-app notification still succeeds. |
| MinIO down | Upload fails immediately with `503`; the client retries. Files are not queued. |

## Account Deletion
---

A user can delete their own account. It is a **soft delete**, not a row removal.

- On request the account moves to status `DELETED`: login is disabled, the profile disappears from search and from other users (shown as "Deleted user"). The DB row stays for referential integrity — this is why there is no orphaned data anywhere in the system.
- **Cascade:** the user's ACTIVE orders auto-close; their responses remain (attributed to "Deleted professional", chat disabled); their messages remain readable for the other party (sender shown as "Deleted user"); reviews they *wrote* are **kept and anonymized** ("from a deleted user") and still count toward professionals' ratings; reviews *about* them remain.
- **PII scrub:** name / email / phone / avatar / bio are set to null / placeholder.
- **Grace period:** `deletion_grace_days` (default 30, admin-configurable). Logging in during the grace period reactivates the account. After it expires the PII is permanently purged and the account cannot be recovered.
- A user **cannot** delete their account while under an active ban/suspension or with an open report against them — the moderation record is preserved and a ban is not cleared by deletion.


## Security & Data Access
---

- Passwords are **never** visible to anyone (including admins/moderators) in plaintext — only the bcrypt hash is ever stored or seen
- There is no payment system on the platform at all, so there is nothing like "payment methods" for an admin to (not) see
- Phone numbers are shown to **anyone** viewing the profile, if the user chose to put one in their profile — not masked, not restricted to a matched professional only
- The email shown on a profile is only the explicit **public contact email field** the user filled in on their profile — not their private account/login email
- API versioning: the platform only ever supports the **current** version — no v1/v2 coexistence or deprecation timeline, this being a pet project
- Simultaneous sessions from multiple devices are allowed — logging in elsewhere does not kick out an existing session
- No geo-blocking and no automatic "suspicious activity" triggers in MVP
- **CSRF tokens are not needed** — the API is authenticated with a JWT bearer token in the request header, and the refresh token is kept in client storage / the request body, not in a cookie
- **CORS:** an explicit origin allowlist supplied via an environment variable (dev: the frontend's localhost ports; prod: e.g. `https://app.profinder.example`). No `*`. `Allow-Credentials: false` (Bearer, not cookies). Configured in `application.properties`.
- **Rate limiting:** unauthenticated endpoints are limited per IP (login also per email); authenticated endpoints per user — a global 1000/hour (NFR-Sec-7) plus tighter per-endpoint write caps (create order 20/h, response 60/h, message 120/h, review 20/h, upload 30/h, report 1/day). Redis counters, `429` + `Retry-After`. A coarse per-IP limit at Nginx is the first line. All limits are admin-configurable.
- **PII in logs:** passwords and tokens are never logged; email / phone / other sensitive fields are masked ([2 - Requirements.md § NFR-Sec-8](2%20-%20Requirements.md)). Log retention 30 days.


## File Storage
---

- MinIO (S3-compatible) is the storage backend for MVP.
- **Per-type size limits** (all admin-configurable): avatar 5 MB, portfolio photo 15 MB, review evidence photo 10 MB, report evidence file 10 MB (~20 files), message image 10 MB, message other file 25 MB.
- **Total per user:** a config knob `max_total_storage_per_user_mb`, set to unlimited in MVP.
- **Type validation by magic bytes** (not extension / client Content-Type): avatar / portfolio / review photos → images only; report evidence → images + `application/pdf` + `text/plain`; message attachments → all types except executables.
- **EXIF / all metadata is stripped** from images on ingestion (privacy — EXIF often carries GPS). Orientation is applied to the pixels, then the tag is dropped.
- **Image pipeline:** re-encode, JPEG quality 82, keep original format; avatar → 512×512 center-crop + 128×128 thumbnail; other images → max 2048 px long edge, single version.
- When a file is deleted, it is removed from storage **immediately** — no delayed cleanup.
- Backups of stored files are not needed for MVP.
- **Not implemented in MVP:** virus scanning on upload, and a max-concurrent-uploads limit.
- CDN for served files stays a Phase 2 item (NFR-Deploy-7). In MVP files are served by MinIO / an Nginx proxy with `Cache-Control: public, max-age=31536000, immutable` (file keys are unique).


## Caching
---

Redis is the cache layer. Pattern: **cache-aside** (read → Redis, miss → DB + write-back with TTL; write → delete the key). All keys are prefixed `v1:` so a DTO-shape change can abandon old keys.

| What | Key | TTL | Invalidation |
|---|---|---|---|
| Professional profile | `v1:profile:pro:{id}` | 30 min | delete-on-write (after DB commit) + enqueue `search.professional_reindex` |
| Customer profile | `v1:profile:cust:{id}` | 30 min | delete-on-write |
| Search result page | `v1:search:…` (hashed filters) | 60 s | none — expires |
| Session revocation | `v1:denylist:{user_id}` | 900 s | set on ban / suspension / role change / password change / logout-all |
| Unread-count (optional) | `v1:notif:unread:{user_id}` | short | on new notification / mark-read |

- **Professional rating is not cached** — it is a denormalized column recalculated synchronously on every review write (see [Rating](#professional-rating)).
- **On a review posted/edited/deleted:** recalculate `rating_avg` / `review_count` → `DEL v1:profile:pro:{id}` → enqueue `search.professional_reindex`. The search cache is left to expire.
- **Not cached:** messages, notification lists, reports (low read volume / must be exact).
- Configurable: `profile_ttl` (30m), `search_ttl` (60s), `session_ttl` (15m).


## Complete Business Logic Summary
---

**Order Lifecycle:** DRAFT → ACTIVE → CLOSED. That's it — no DELETED status, no soft delete, no reopening.

**Key Design Principles:**
- ✅ Simple, clean 3-status state machine (DRAFT, ACTIVE, CLOSED)
- ✅ Order stays ACTIVE during communication and work — customer decides when to close it
- ✅ Closing is final; there is no reopening and no delete/recovery flow for published orders
- ✅ One editable response per professional per order (sealed bids), no withdrawal mechanism
- ✅ Reviews are fully independent of orders — a customer can review any professional at any time
- ✅ Report system is separate and independent (part of Core API)
- ✅ Ratings are a single, simple metric: average of 1–5 stars, with outlier filtering, recalculated on deletion
- ✅ No professional verification/"Verified badge" system — removed from scope entirely
- ✅ Moderation for MVP is manual and reactive (report-triggered) across reviews, messages, and portfolio photos
- ✅ No notification "quiet hours" — notifications are always delivered immediately
- ✅ Messages stored permanently until a user deletes them; deletion is immediate in the UI, with a short grace period retained for moderator dispute review
- ✅ Professional can have multiple cities and unlimited categories
- ✅ Reputation is a single number (1–5) for both customers and professionals — no secondary metrics like response rate

**Core Statuses:**
- **DRAFT** — order not published, customer-only, fully editable, hard-deletable
- **ACTIVE** — open for responses, visible to professionals, includes chat/negotiation, editable, extendable
- **CLOSED** — customer manually closes when work is done or no longer needed; final state, moved to history

**Report System:** Part of Core API
- Separate from order lifecycle, no order required
- Statuses: submitted → under_review → resolved
- No SLA, no assignment, no priority levels — shared queue, moderator's own pace
- Moderator handles decisions and actions; suspension is a moderator-chosen 1–365 days (presets 3/7/14/30) with auto-return on expiry; bans are permanent by default but **can be appealed** within 30 days, reviewed by a different moderator (the one exception to "no appeal")

**Added to MVP since v3.1:** account deletion (soft delete + PII scrub + 30-day grace).

**Removed from MVP since v3.1:** 2FA (deferred to Phase 2).

**Explicitly cut from MVP (revisit later):** typing indicators, notification grouping, per-event notification sound customization, virus scanning on upload, a max-concurrent-uploads limit, thumbnail generation beyond the avatar, email digest (FR-Notif-6), 2FA, appeals for anything other than a ban, saved search preferences on the backend, "featured" / promoted listings, distance/radius search, "response time" metric, per-criteria rating breakdown, professional verification / "Verified badge".

**Deferred to Phase 2:** automated content moderation (spam/profanity/ML/image classification), read replicas, RabbitMQ cluster, queue-depth auto-scaling, CDN, file backups / DR, GDPR data export, runtime settings table, notification digest.

---

## Changelog
---

| Version | Date | Change |
|---|---|---|
| 3.6 | 2026-09-10 | Standardised to the shared doc format (title, metadata, separators, changelog). Removed a line duplicated verbatim in *Security & Data Access* ("API versioning: current version only"). Repointed the dead file-5 links (a "Questions" doc that no longer exists) to [2 - Requirements.md](2%20-%20Requirements.md) / this file's own sections. |
| 3.5 | 2026-09-07 | Reconciled the Events section with [9 - Event Catalog.md](9%20-%20Event%20Catalog.md): dropped `order.draft_created`; split `order.response_received` into `response.created` + `response.edited` (in-app only); added `message.sent` / `message.deleted` / `report.created` / `account.deletion_finalized` / `auth.*`; renamed the reindex events; search-index queue = unlimited retry, no DLQ. |
| 3.4 | 2026-09-04 | Closed 2 gaps found while state-diagramming: professional can't drop below 1 category / 1 city (replace-only, `ACTIVE` is sticky); a `PENDING_VERIFICATION` account can't be suspended/banned. |
| ≤3.3 | 2026-08 → 2026-09-04 | Block A gaps A1–A5 closed (chat decoupled from orders, flat categories, USD, dropped `preferred professional level` + order lat/long, customer reliability rating, Admin = top-level Moderator). Account deletion added; 2FA removed. Earlier revisions built the order lifecycle, rating model, report system, auth, profiles, search, messaging, notifications, concurrency and caching sections. |
