# ProFinder — Requirements

- **Version:** 1.6
- **Date:** 2026-09-10
- **Status:** In Development
- **Purpose:** Complete MVP requirements — functional requirements, non-functional requirements, use cases, scope, and the technology stack. Backend focus.

---

## 1. Executive Summary
---

ProFinder is a free marketplace platform that connects customers with professional service providers (plumbers, electricians, etc.). The platform facilitates discovery, communication, and reputation management between two parties. Payment processing is handled directly between customers and professionals, not through the platform.

**Core Value Proposition:**
- Customers: Easy way to find trusted professionals
- Professionals: Access to customers seeking their services
- Platform: Commission-free marketplace with premium monetization options


## 2. Functional Requirements (FR)
---

### FR-Auth: Authentication & Registration
---

| ID | Requirement | Description |
|----|-------------|-------------|
| FR-Auth-1 | Email Registration | Users register with email + password. Email verified within 24h (link, admin-configurable). Account usable in a limited `PENDING_VERIFICATION` state until confirmed (no publishing/responding/messaging/reviews/search visibility). Lockout: 5 failed logins / 15 min → 15 min temporary block. |
| FR-Auth-2 | Google OAuth | Users register/login via Google (minimal scopes). Strict 1:1 account↔Google link. Unlink allowed once a password is set. First login without a role → mandatory role-selection screen. |
| FR-Auth-3 | User Roles | System supports Customer, Professional, and Moderator roles — **no separate Admin role** ("Admin" = top-permission Moderator, see FR-Admin note). Role is chosen once and is **immutable in MVP** (support-only change). |
| FR-Auth-4 | Profile Completion | Customers: no completeness gate at all. Professionals: profile is `INCOMPLETE` (not searchable, cannot respond) until name + ≥1 category + ≥1 city are set, then `ACTIVE`. |
| FR-Auth-5 | Session Management | JWT access token (15 min, carries role + account_status claims) + sliding refresh token (7 days, rotation + reuse detection, 30-day absolute cap). Refresh token in client storage / request body — no cookies. Per-request Redis denylist check for instant revocation. |
| FR-Auth-6 | Account Deletion | Users can soft-delete their own account: login disabled, profile hidden, PII scrubbed, DB row kept (no orphaned data). 30-day grace (reversible by logging in), then permanent PII purge. Blocked while under an active ban or open report. |


### FR-Profile: Customer Profile Management
---

| ID | Requirement | Description |
|----|-------------|-------------|
| FR-Profile-C1 | Profile Fields | Customer provides: name, avatar, phone, location (city/country), bio, preferred categories, nickname |
| FR-Profile-C2 | Profile Editing | Customers can edit their profile information anytime |
| FR-Profile-C3 | Order History | Customers can view their own order history (private, visible only to them) |
| FR-Profile-C4 | Reputation | Customers receive a "reliable customer" rating: a professional can leave one editable 1–5 review per customer (same mechanics as customer→professional reviews), no customer reply in MVP |
| FR-Profile-C5 | Profile Visibility | Limited profile information visible to professionals |


### FR-Profile-P: Professional Profile Management
---

| ID | Requirement | Description |
|----|-------------|-------------|
| FR-Profile-P1 | Profile Fields | Professional provides: name, photo, bio, service categories (multiple), years of experience, hourly rate, location/city, phone, email, nickname |
| FR-Profile-P2 | Service Categories | Professionals can select multiple service categories they offer |
| FR-Profile-P3 | Portfolio | Professionals can upload photos of past work (optional, published immediately; moderator can remove a photo reactively if it's reported) |
| FR-Profile-P4 | Availability | Professionals set working hours (single fixed schedule) and a manual available/unavailable toggle that directly controls search visibility |
| FR-Profile-P5 | Profile State | `INCOMPLETE` (log in + edit only) → `ACTIVE` (searchable, can respond) once name + ≥1 category + ≥1 city are set. Missing optional fields do not penalise ranking. |
| FR-Profile-P6 | Rating Display | Simple mean of 1–5 star reviews (outliers filtered), shown from the 1st review (0 when none). Stored as denormalized `rating_avg` / `review_count`, recalculated synchronously on every review write. |

**Note:** Professional verification ("Verified" badge, admin review of professionals) and response-rate tracking are **not part of the scope** — reputation is a single 1–5 star rating with no secondary metrics.


### FR-Orders: Order Management
---

| ID | Requirement | Description |
|----|-------------|-------------|
| FR-Orders-1 | Order Creation | Customers can create orders with: service category, preferred date/time, location, work description, budget/price range (USD) |
| FR-Orders-2 | Order Statuses | Three statuses: DRAFT (unpublished), ACTIVE (open for responses), CLOSED (final, no reopening) |
| FR-Orders-3 | Order Editing | Customers can edit orders anytime (DRAFT and ACTIVE states); editing an ACTIVE order preserves existing responses and notifies responding professionals |
| FR-Orders-4 | Draft Deletion | Customers can hard-delete their own DRAFT orders (no soft-delete, no recovery). Published (ACTIVE/CLOSED) orders cannot be deleted — closing is the only way to end them |
| FR-Orders-5 | Order Expiration | Customer sets expiration time for orders (configurable deadline); system notifies 1 day before and at expiration, allows unlimited extensions (configurable duration) or closing |
| FR-Orders-6 | Response Limit | No maximum limit — unlimited professionals can respond to same order |
| FR-Orders-8 | Order History | Orders remain in system history after CLOSED (final state, cannot be reopened) |

*(FR-Orders-7 was dropped during scoping; the ID is intentionally skipped to keep the others stable.)*


### FR-Responses: Professional Responses
---

| ID | Requirement | Description |
|----|-------------|-------------|
| FR-Responses-1 | Send Response | An `ACTIVE` professional can submit **one** response per ACTIVE order in their categories; re-submitting edits it. Rejected on a CLOSED or expired order. |
| FR-Responses-2 | Include Quote | Response includes a price quote + message. Editable while the order is ACTIVE (shows "(edited)"). |
| FR-Responses-3 | Response Visibility | Customer sees **all** responses (newest first, re-sortable by price/rating), paginated 20/page. **Sealed bids** — professionals never see each other's responses, quotes, or the response count. |

**Note:** There is no response withdrawal — a submitted response simply stands (editable while the order is ACTIVE). Responses from a professional who is later banned/suspended/deleted are filtered from the customer's list until reinstated.


### FR-Communication: Direct Messaging and Negotiation
---

| ID | Requirement | Description |
|----|-------------|-------------|
| FR-Communication-1 | Open Communication | Customer can message any professional at any time — from search/profile or after a response to an order. Chat is not gated by an order response; keyed by (customer, professional) pair, not by order. |
| FR-Communication-2 | Multi-party Messaging | Customer can communicate with multiple professionals simultaneously for same order |
| FR-Communication-3 | Negotiations | Customer and professional can negotiate terms, discuss details, and coordinate work via chat |
| FR-Communication-4 | Chat Record | Messages stored until a user deletes them; a deleted message disappears fully from the UI but is kept briefly server-side for moderator review before permanent purge |
| FR-Communication-5 | Message History | Complete message history retained for order timeline and dispute resolution |


### FR-Messaging: In-App Messaging
---

| ID | Requirement | Description |
|----|-------------|-------------|
| FR-Messaging-1 | Text Messages | Full text-based messaging between customer and professional |
| FR-Messaging-2 | File Sharing | Users can share files, photos, videos in messages |
| FR-Messaging-3 | Message History | Message history retained for active orders |
| FR-Messaging-4 | Notifications | Users receive push notifications for new messages |
| FR-Messaging-5 | Future Extensibility | System architecture supports adding voice/video calls later |
| FR-Messaging-6 | Moderator Access | Moderators can view messages in case of disputes |


### FR-Reviews: Reviews & Ratings
---

| ID | Requirement | Description |
|----|-------------|-------------|
| FR-Reviews-1 | Post Review | Customers can leave a review on any professional's profile at any time — reviews are fully decoupled from orders, no order relationship is required |
| FR-Reviews-2 | Rating Scale | 1-5 star rating system (required); review text is optional |
| FR-Reviews-3 | Review Content | Reviews can include: stars, text, photos as evidence |
| FR-Reviews-4 | Professional Response | Professionals can reply to any review; customer can reply back (threaded) |
| FR-Reviews-5 | Review Moderation | Published immediately; moderator acts reactively only when a review is reported (MVP). Automatic flagging is a future phase |
| FR-Reviews-6 | Negative Reviews | Customers can freely leave negative reviews (1-2 stars) |
| FR-Reviews-7 | Self-Employed Discovery | Customers can leave reviews for professionals found outside the platform |
| FR-Reviews-8 | Rating Calculation | Simple average of all non-deleted, non-outlier reviews; recalculated whenever a review is added or deleted |
| FR-Reviews-9 | Review Visibility | Reviews visible on professional profile publicly |
| FR-Reviews-10 | One Review Per Professional | A customer effectively has one review per professional — posting again edits the existing review |


### FR-Search: Search & Discovery
---

| ID | Requirement | Description |
|----|-------------|-------------|
| FR-Search-1 | Filter by Category | Search results filterable by service category (flat list, exact match — no hierarchy) |
| FR-Search-2 | Filter by Location | Search results filterable by city (exact match — no distance/radius search) |
| FR-Search-3 | Filter by Rating | Search results filterable by minimum rating, 0.0-5.0 in 0.1 increments |
| FR-Search-4 | Filter by Price | Search results filterable by hourly rate range |
| FR-Search-5 | Filter by Availability | Search results filterable by current availability (manual on/off toggle) |
| FR-Search-7 | Filter by Experience | Search results filterable by exact years of experience |
| FR-Search-8 | Multi-Filter | Multiple filters can be combined |
| FR-Search-9 | Sort Options | Default = "Recommended" (available first → Bayesian `sort_rating` → review_count → recency). Explicit: rating (raw), price, popularity (`raw_mean × log10(review_count+1)`), newest. Text query → BM25 relevance × `ln(1+sort_rating)`. No "response time" sort. |
| FR-Search-10 | Search Results | Card served straight from Elasticsearch: `id`, `display_name`, `avatar_thumb_url`, `rating` + `review_count`, `hourly_rate` (`city` / `available` optional). Everything else loads on profile open. Pagination 20/page (configurable, max 100). |
| FR-Search-11 | Index Sync | Professionals & orders indexed event-driven via RabbitMQ (transactional outbox) on any relevant change + a nightly full reindex. If Elasticsearch is down: `503 SEARCH_UNAVAILABLE`, no PostgreSQL fallback. Result pages cached 60s by filter hash. |

**Note:** There is no professional verification, so no verification filter. There is no distance-based search — only exact city matching. No "featured" / promoted / pay-to-rank listings.

*(FR-Search-6 was dropped during scoping; the ID is intentionally skipped.)*


### FR-Notifications: Notifications System
---

| ID | Requirement | Description |
|----|-------------|-------------|
| FR-Notif-1 | Event Types | Notifications for: new order match, professional response, order status changes, messages, reviews & review replies, moderation decisions, account deletion/reactivation. (No payment events — there are no payments.) |
| FR-Notif-2 | Push Notifications | Firebase Cloud Messaging directly. Device-token table with refresh (`onNewToken`) and invalidation (FCM `UNREGISTERED`, logout, 60-day inactivity). Deep-link payload `{type, entity_type, entity_id, url}`. |
| FR-Notif-3 | In-App Notifications | PostgreSQL-backed notification center, paginated 20/page, unread badge, mark-(all-)read persisted. Purge **read** notifications > 90 days (configurable); unread kept indefinitely. Notification center only — no toast. |
| FR-Notif-4 | User Preferences | `notification_preferences(user_id, event_type, channel)` matrix, `channel ∈ {push, email, in_app}`, all on by default. |
| FR-Notif-5 | Email Delivery | Real-time per event. `multipart/alternative` (HTML + text), branded Qute template. Unsubscribe per-type + "unsubscribe from all" footer link. |
| FR-Notif-6 | Email Digests | **Phase 2 — not in MVP.** |

**Note:** There are no "quiet hours" — explicitly rejected. Notifications are always delivered immediately, regardless of time of day. Notification grouping and per-event sound customization are also out of MVP.


### FR-Moderator: Moderator Capabilities
---

| ID | Requirement | Description |
|----|-------------|-------------|
| FR-Moderator-1 | User Management | Moderators can view, ban, unban users |
| FR-Moderator-2 | Content Moderation | Delete inappropriate reviews/messages/portfolio photos (reactively, when reported) |
| FR-Moderator-4 | Dispute Resolution | Resolve disputes between customer and professional |
| FR-Moderator-5 | Data Access | View user data when needed for moderation |
| FR-Moderator-6 | Order Deletion | Delete problematic orders |
| FR-Moderator-7 | User Messages | Send messages to users (warnings, notifications) |
| FR-Moderator-8 | Reports | Generate moderation reports and statistics |
| FR-Moderator-9 | Permission Levels | Multiple moderators with different permission levels (STANDARD / SENIOR — see [11 - Permission Matrix.md](11%20-%20Permission%20Matrix.md)) |
| FR-Moderator-10 | Support Tickets | Users can contact moderators via chat/tickets for help |

*(FR-Moderator-3 was dropped during scoping; the ID is intentionally skipped.)*


### FR-Reports: User & Professional Reporting System
---

**Purpose:** Independent reporting system for policy violations and misconduct (separate from order lifecycle)

| ID | Requirement | Description |
|----|-------------|-------------|
| FR-Reports-1 | Report Types | Users can report: professional conduct violations, customer conduct violations, inappropriate user behavior |
| FR-Reports-2 | Report Creation | Submitter provides: reason, detailed description, optional evidence (messages, photos, links) |
| FR-Reports-3 | Report Reasons | Predefined reasons: no-show, poor quality, non-payment, harassment, scam, inappropriate content, other |
| FR-Reports-4 | Evidence Attachment | Users can attach message links, photos, text descriptions as evidence |
| FR-Reports-5 | Report Status | Reports tracked through: submitted → under_review → resolved |
| FR-Reports-6 | Moderator Dashboard | Moderator can view all pending reports with associated order history and messages |
| FR-Reports-7 | Moderator Actions | Moderator can: dismiss; issue a **warning** (notice only, no restriction, no auto-escalation — prior count shown); **suspend** for a chosen 1–365 days (presets 3/7/14/30, auto-return on expiry); **ban** (permanent, appealable within 30 days — the only appealable decision — reviewed by a different moderator); **remove content** (immediate, user notified after). Suspending/banning a customer auto-closes their ACTIVE orders. |
| FR-Reports-8 | Moderator Notes | Moderator can add notes/reasoning for each decision |
| FR-Reports-9 | User Notification | Reporter notified when report is resolved with decision (not detailed reasons, just outcome) |
| FR-Reports-10 | Reported User Notification | Reported user notified of consequences: warning received, suspension start date, or ban reason |
| FR-Reports-11 | Report History | All reports and decisions logged for audit trail and pattern detection |
| FR-Reports-12 | Duplicate Prevention | One report per `(reporter, reported_user)` pair per rolling 24 hours, regardless of reason; a second attempt is rejected with a "you already reported this user today" message. |
| FR-Reports-13 | Ban Appeal | A banned user files one appeal within 30 days via a dedicated screen (the only action available to them). Reviewed by a moderator other than the one who issued the ban. No counter-appeal by the reporter. |


### FR-Admin: Admin Features
---

| ID | Requirement | Description |
|----|-------------|-------------|
| FR-Admin-1 | Service Categories | Admin can add/edit/delete service categories |
| FR-Admin-2 | Analytics Dashboard | View system metrics: users, orders, revenue, active listings |
| FR-Admin-3 | System Logs | Access system logs for debugging |

**Note:** There is no separate Admin role in MVP. These capabilities are performed by a Moderator with the top permission level (FR-Moderator-9). A dedicated fine-grained permission system is deferred to a later phase.


## 3. Non-Functional Requirements (NFR)
---

### NFR-Performance
---

| ID | Requirement | Target | Rationale |
|----|-------------|--------|-----------|
| NFR-Perf-1 | API Response Time | < 500ms median, < 1500ms 95th percentile | Acceptable for web/mobile MVP |
| NFR-Perf-2 | Search Query Time | < 2 seconds | Complex queries OK for MVP |
| NFR-Perf-3 | Authentication | < 500ms | Must be snappy |
| NFR-Perf-4 | Caching Strategy | Redis with TTL 15-30min | Cache profiles, orders, searches |
| NFR-Perf-5 | Database Indexing | Proper indexes on all search fields | Required for performance |

**Status:** ✅ Not critical for MVP (performance scales with load)


### NFR-Reliability
---

| ID | Requirement | Decision | Rationale |
|----|-------------|----------|-----------|
| NFR-Rel-1 | Uptime Target | 95% acceptable (MVP phase) | No SLA needed initially |
| NFR-Rel-2 | Backups | Not required for MVP | Can add in Phase 2 |
| NFR-Rel-3 | Geo-redundancy | Single datacenter | One region sufficient |
| NFR-Rel-4 | Database Failure | Manual recovery | Manual recovery is acceptable for MVP phase |
| NFR-Rel-5 | Monitoring | Real-time error alerts required | You get notified on critical errors |

**Status:** ✅ MVP pragmatic approach - reliability can improve in Phase 2


### NFR-Security
---

| ID | Requirement | Implementation | Notes |
|----|-------------|-----------------|-------|
| NFR-Sec-1 | Password Hashing | bcrypt (cost factor 12) | Industry standard, good security/speed balance |
| NFR-Sec-2 | Authentication | JWT: 15min access (role + account_status in claims, verified locally), 7day sliding refresh with rotation + reuse detection + 30d absolute cap. Refresh token in client storage / request body, never a cookie. Per-request Redis denylist for instant revocation. |
| NFR-Sec-3 | 2FA | **Not in MVP** — removed from scope (2026-08-27). Architecture leaves room for TOTP; deferred to Phase 2. |
| NFR-Sec-4 | Attack Prevention | SQL injection, XSS, Rate limiting, DDoS — via security libraries. **CSRF: not applicable** (Bearer-token auth, no cookies). |
| NFR-Sec-5 | HTTPS/SSL | Required in production, optional in dev | Use Let's Encrypt (free) |
| NFR-Sec-6 | Data Encryption | Sensitive data only (passwords, tokens) | Use PostgreSQL encryption at rest |
| NFR-Sec-7 | Rate Limiting | Per-IP for unauthenticated endpoints, per-user for authenticated (1000/hour) + tighter per-endpoint write caps (order 20/h, response 60/h, message 120/h, review 20/h, upload 30/h, report 1/day). Redis counters, `429` + `Retry-After`. Coarse per-IP layer at Nginx. All configurable. |
| NFR-Sec-8 | Data Access | Strict - nobody sees sensitive data | Even admins can't see plaintext passwords. PII masked in logs (30-day retention). |
| NFR-Sec-9 | Secret Management | Environment variables (.env) | Never commit secrets to git |
| NFR-Sec-10 | GDPR/Compliance | Minimal (pet project) | Account deletion + PII scrub covers "right to be forgotten"; data export is Phase 2; static privacy/ToS pages, no legal review |
| NFR-Sec-11 | CORS | Explicit origin allowlist via env var, no `*`, `Allow-Credentials: false` | Configured in application.properties |

**Status:** ✅ CRITICAL - Security implemented properly from start


### NFR-Scalability
---

| ID | Requirement | MVP Decision | Phase 2+ Plan |
|----|-------------|--------------|---------------|
| NFR-Scale-1 | Growth Strategy | Single VPS (vertical) for infra, but every service is stateless behind a load balancer from day 1 — adding replicas needs no API redesign | Add more replicas / move to Kubernetes when needed |
| NFR-Scale-2 | Load Balancing | **Yes, from day 1** — Nginx (L7) load-balances across instances of each service; corrected 2026-09-05, previously said "not needed" which contradicted [3 - System Design.md § Nginx Gateway](3%20-%20System%20Design.md) | Swap Nginx for a dedicated LB / API gateway if needed at scale |
| NFR-Scale-3 | File Storage | MinIO (S3-compatible) | Easy to migrate to AWS S3 later |
| NFR-Scale-4 | Database | PostgreSQL only | Optional: MongoDB for analytics later |
| NFR-Scale-5 | Architecture | Core API (centralizes most business logic) + specialized microservices (Auth, Messaging, Notification, Moderation) | Split by concern, not by bottleneck — Auth/Messaging/Notification/Moderation are separate for security isolation, WebSocket, async delivery, and content checks respectively. Extract more out of Core API only if a bottleneck is found there. |
| NFR-Scale-6 | Containers | Docker required | Kubernetes when scaling horizontally |
| NFR-Scale-7 | Caching | Redis in-memory | Add persistence if needed |

**Status:** ✅ Designed for growth — Core API centralizes most business logic; Auth/Messaging/Notification/Moderation are already split out as their own services


### NFR-Maintainability
---

| ID | Requirement | Target | Implementation |
|----|-------------|--------|-----------------|
| NFR-Maint-1 | Code Quality | SOLID principles | Design patterns, code reviews |
| NFR-Maint-2 | Unit Testing | 80%+ coverage | JUnit 5 + Mockito on business logic; JaCoCo coverage gate (see § Tech Stack) |
| NFR-Maint-3 | Integration Testing | All API endpoints | At least 1 test per endpoint |
| NFR-Maint-4 | CI/CD | Full automation | Git → Test → Build → Deploy (< 5 min) |
| NFR-Maint-5 | Documentation | Comprehensive | README, API docs, Architecture, Contributing |
| NFR-Maint-6 | Logging | Detailed (DEBUG, INFO, WARN, ERROR) | ELK Stack or Loki for storage |
| NFR-Maint-7 | Linting | Automated formatting | Checkstyle (style rules) + Spotless (auto-format), enforced in CI |
| NFR-Maint-8 | Code Comments | Only for non-obvious logic | Self-documenting code preferred |

**Status:** ✅ Very important - focus on clean, testable code from day 1


### NFR-Monitoring & Logging
---

| ID | Requirement | Tool | Details |
|----|-------------|------|---------|
| NFR-Mon-1 | Metrics Collection | Prometheus | CPU, memory, disk, API latency, errors |
| NFR-Mon-2 | Dashboards | Grafana | Real-time visualization of metrics |
| NFR-Mon-3 | Log Storage | ELK Stack or Loki | Centralized logging for debugging |
| NFR-Mon-4 | Alerting (Phase 1) | Manual review | You read logs daily/as needed |
| NFR-Mon-5 | Alerting (Phase 2+) | Email/Slack notifications | Automatic alerts on errors > 1% |
| NFR-Mon-6 | Log Levels | Debug, Info, Warn, Error | Track user actions, API calls, errors |
| NFR-Mon-7 | Log Retention | 30 days minimum | Balance storage vs. debugging needs |

**Status:** ✅ Required - essential for pet project debugging


### NFR-Deployment & Infrastructure
---

| ID | Requirement | MVP Setup | Future |
|----|-------------|-----------|--------|
| NFR-Deploy-1 | Hosting | Self-hosted VPS ($10-50/month) | Migrate to AWS/Google Cloud if needed |
| NFR-Deploy-2 | Containerization | Docker + Docker Compose | K8s when scaling |
| NFR-Deploy-3 | Orchestration | Docker Compose (one server) | Kubernetes for multiple servers |
| NFR-Deploy-4 | Database | PostgreSQL 14+ | Replicas + read-only instances later |
| NFR-Deploy-5 | Cache | Redis (in-memory) | Add persistence if needed |
| NFR-Deploy-6 | Secrets | Environment variables | HashiCorp Vault or AWS Secrets Manager (Phase 2) |
| NFR-Deploy-7 | Image Uploads | MinIO (self-hosted S3) | AWS S3 or CloudFront CDN (Phase 2) |

**Status:** ✅ Cloud-ready with Docker from start


### NFR-Usability
---

| ID | Requirement | MVP | Notes |
|----|-------------|-----|-------|
| NFR-UX-1 | Platform Support | REST API (backend only) | Web/Mobile clients separate |
| NFR-UX-2 | Offline Support | Not needed | Always-online marketplace |
| NFR-UX-3 | Accessibility | Basic (Phase 2) | WCAG 2.1 Level A for frontend later |
| NFR-UX-4 | Internationalization | English + extensible | Support other languages via i18n later |
| NFR-UX-5 | Load Speed | Adequate (< 2s target) | Don't over-optimize MVP |

**Status:** ⚠️ Low priority for backend - focus on web/mobile frontend later


## 4. Key Use Cases
---

### UC-1: User Registration & Profile Setup
---

**Actors:** New Customer / Professional

**Precondition:** User has an account email or Google

**Main Flow:**
1. User clicks "Sign Up"
2. Selects role (Customer or Professional)
3. Authenticates via email or Google
4. Fills in required profile information
5. Verifies email (if email signup)
6. System creates user account and profile
7. User redirected to dashboard

**Post-condition:** User account created, ready to use platform


### UC-2: Professional Discovers Orders
---

**Actors:** Professional

**Precondition:** Professional logged in, profile complete

**Main Flow:**
1. Professional navigates to "Available Orders"
2. Views list of open orders (ACTIVE status)
3. Filters by category, location, price, other parameters
4. Views order details and customer profile
5. Reads job description and requirements

**Post-condition:** Professional identifies suitable orders


### UC-3: Professional Responds to Order
---

**Actors:** Professional

**Precondition:** Professional viewing an ACTIVE order

**Main Flow:**
1. Professional clicks "Respond to Order"
2. Enters quote/price for the job
3. Optionally adds message to customer
4. Submits response
5. System notifies customer about new response
6. Professional's response appears in customer's list

**Post-condition:** Professional response recorded, customer notified


### UC-4: Customer Creates Order
---

**Actors:** Customer

**Precondition:** Customer logged in, profile complete

**Main Flow:**
1. Customer clicks "Create Order"
2. Selects service category needed
3. Enters job details: description, location, preferred date/time
4. Sets budget/price range
5. Optionally sets expiration period for order
6. Publishes order
7. Order transitions to ACTIVE status
8. Matching professionals notified

**Post-condition:** Order published, visible to professionals in category


### UC-5: Customer Communicates with Responding Professionals
---

**Actors:** Customer, Professional

**Precondition:** Order has responses (ACTIVE status with responses)

**Main Flow:**
1. Customer views list of responding professionals
2. Reviews their profiles, ratings, responses, prices
3. Opens direct chat with interested professionals
4. Can communicate with multiple professionals simultaneously
5. Negotiates terms, discusses schedule, price, specific requirements
6. Exchanges messages, photos, files for coordination
7. Customer and professional continue chatting as work progresses

**Post-condition:** Customer communicates with one or multiple professionals for the order


### UC-6: Customer Closes Order and Leaves Review
---

**Actors:** Customer, Professional

**Precondition:** Customer decides work is complete or order is no longer needed

**Main Flow:**
1. Customer manually closes the order (ACTIVE → CLOSED, final — cannot be reopened)
2. Order archived and removed from active list
3. Customer can leave a review for any professional (reviews are independent of orders — not limited to professionals from this order, and not required to happen right after closing)
4. Review includes: 1-5 stars (required) + text (optional) + optional photos
5. Customer can edit or delete review anytime
6. Professional receives notification of review
7. Professional can respond to review; customer can reply back
8. Review appears on professional's profile immediately (moderated reactively, only if reported)

**Post-condition:** Order closed (final state), reviews published (if any), professional ratings updated


### UC-7: Search and Discover Professionals
---

**Actors:** Customer

**Precondition:** Customer browsing for professionals or services

**Main Flow:**
1. Customer navigates to "Find Professional"
2. Enters service category (e.g., "Plumbing")
3. Applies filters: city, rating, price range, experience, availability
4. Sorts results: by rating/price/popularity
5. Views professional profiles in results
6. Clicks on profile to see details, reviews, portfolio, ratings
7. Can initiate chat, create order, or contact directly

**Post-condition:** Customer discovered professionals matching their criteria


### UC-8: Moderator Reviews and Acts on Reports
---

**Actors:** Moderator

**Precondition:** User report submitted by customer or professional

**Main Flow:**
1. Moderator views pending report in dashboard
2. Reviews evidence: messages, photos, order history
3. Reads reporter and reported user explanations
4. Checks user history and patterns
5. Makes decision: dismiss, warn, suspend, ban, or remove content
6. Sends decision notification to both parties
7. Possible actions: issue warning, suspend account, ban account, remove review/message
8. Documents decision and reasoning in system

**Post-condition:** Report resolved, parties notified, action taken if needed


### UC-9: Professional Uploads Portfolio
---

**Actors:** Professional

**Precondition:** Professional in profile edit mode

**Main Flow:**
1. Professional navigates to Portfolio section
2. Uploads photos of past work (unlimited count)
3. Adds optional captions (up to 2000 characters)
4. Photos appear on the professional's profile immediately — no pre-publish moderation queue
5. If a photo is later reported, a moderator reviews and can remove it with feedback (reactive moderation only)

**Post-condition:** Portfolio photos visible to everyone immediately


## 5. Use Case to Requirements Traceability Matrix
---

| Use Case | FR-Auth | FR-Profile | FR-Orders | FR-Responses | FR-Communication | FR-Messaging | FR-Reviews | FR-Search | FR-Notif | FR-Moderator |
|----------|---------|-----------|-----------|--------------|------------------|--------------|-----------|----------|-----------|--------------|
| UC-1: Registration | ✓ | ✓ | - | - | - | - | - | - | - | - |
| UC-2: Discover Orders | ✓ | ✓ | ✓ | - | - | - | - | ✓ | - | - |
| UC-3: Respond | ✓ | ✓ | ✓ | ✓ | - | - | - | - | ✓ | - |
| UC-4: Create Order | ✓ | ✓ | ✓ | - | - | - | - | - | ✓ | - |
| UC-5: Communicate & Negotiate | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | - | - | ✓ | - |
| UC-6: Complete & Review | ✓ | ✓ | ✓ | - | - | ✓ | ✓ | - | ✓ | - |
| UC-7: Search Professionals | ✓ | ✓ | - | - | - | - | ✓ | ✓ | - | - |
| UC-8: Resolve Dispute | ✓ | ✓ | ✓ | - | - | ✓ | ✓ | - | ✓ | ✓ |
| UC-9: Upload Portfolio | ✓ | ✓ | - | - | - | - | - | - | - | ✓ |


## 6. Project Scope
---

### Full Feature Set - Production Ready
---

**Included:**
- ✅ User authentication (email + Google OAuth; no 2FA in MVP)
- ✅ Customer & Professional profiles (professional 2-state INCOMPLETE/ACTIVE)
- ✅ Account soft-deletion (PII scrub + 30-day grace)
- ✅ Order creation & management (DRAFT/ACTIVE/CLOSED — no delete, no reopening)
- ✅ Professional responses — one editable response per professional per order, sealed bids, no withdrawal
- ✅ In-app messaging (text now; file sharing is a later iteration)
- ✅ Advanced search and filtering (Elasticsearch, event-driven sync via outbox, "Recommended" default sort)
- ✅ Reviews and ratings (1-5 stars, decoupled from orders, reactive moderation)
- ✅ Complete moderator system (warn / suspend 1–365d / permanent+appealable ban, dispute resolution, reactive content moderation)
- ✅ Notifications (push via FCM, email real-time, in-app — no quiet hours, no digest)
- ✅ Portfolio (unlimited photos, reactive moderation)
- ✅ Asynchronous task processing (RabbitMQ + transactional outbox)

**Out of Scope (Future Additions):**
- ❌ Payment processing (direct between users)
- ❌ 2FA / TOTP
- ❌ Automated content moderation (spam / profanity / ML toxicity / image classification) — MVP moderation is manual & reactive
- ❌ Email digest (FR-Notif-6), notification grouping, per-event sound
- ❌ Advanced analytics & recommendation engine
- ❌ Voice/video calls & file sharing in chat (architecture supports WebSocket foundation)
- ❌ Mobile native apps (REST API supports any client)
- ❌ Advanced fraud detection AI
- ❌ Subscription billing system, featured / promoted listings
- ❌ CDN, file backups / DR, read replicas, RabbitMQ cluster, GDPR data export, runtime settings table


## 7. Monetization Model
---

**Selected:** Variant B - Free Marketplace

**How it works:**
- Platform does NOT process payments
- Customers and professionals pay each other DIRECTLY (bank transfer, cash, third-party apps)
- Potential future monetization (**none of this is in MVP**): premium profile listings, featured/boosted listings, ad banners, directory subscriptions
- **MVP has no monetization and no paid ranking** — no "featured" professionals, no pay-to-rank, no promoted results. Search ranking is purely rating/relevance-based.

**No commission on orders** = Professionals keep 100% of payment


### 7.1 Email Verification
---

- Email verification required for all users (confirmation link), regardless of role
- Prevents spam accounts
- Verification link valid for **24 hours** (`email_verification_ttl`, admin-configurable)
- Password-reset link valid for **15 minutes** (kept short — more sensitive)
- Before verification the account works in a limited `PENDING_VERIFICATION` state (see FR-Auth-1)
- Resend limited to 1 / 60s and ≤ 5 / hour; each new link invalidates the previous one

**Note:** There is no professional "Verified" badge / admin verification system — this was explicitly decided against and removed from scope. The only verification in the platform is basic email-address confirmation, identical for customers and professionals.


## 8. Technical Constraints & Final Decisions
---

| Constraint            | Decision                           | Rationale                                  |
| --------------------- | ---------------------------------- | ------------------------------------------ |
| Platforms             | REST API backend only              | Web & mobile clients separate              |
| Focus                 | Clean code, tests, maintainability | Backend-first, MVP priority                |
| **Backend**           | **Java 21 LTS + Quarkus 3.x**      | Fast, lightweight, cloud-native ⭐          |
| **Build Tool**        | **Maven**                          | Standard, widespread, predictable          |
| **Database**          | **PostgreSQL 14+**                 | Relational, robust, JSONB support          |
| **ORM**               | **Hibernate ORM**                  | Standard with Quarkus, proven              |
| **Cache**             | **Redis (Jedis)**                  | In-memory, scalable, fast                  |
| **File Storage**      | **MinIO (S3-compatible)**          | Self-hosted, easy migration to AWS S3      |
| **Message Broker**    | **RabbitMQ**                       | AMQP protocol, reliable message delivery, task queues |
| **API Protocol**      | **REST + JSON**                    | Universal, simple, HTTP standard           |
| **API Docs**          | **OpenAPI/Swagger**                | Auto-generated interactive docs            |
| **Security**          | JWT (Quarkus) + BCrypt + OIDC      | Complete, secure, standard                 |
| **Testing**           | JUnit 5 + Mockito + Testcontainers | 80%+ coverage, realistic integration tests |
| **Logging**           | SLF4J + Logback + JSON             | Structured logs for ELK stack              |
| **Monitoring**        | Prometheus + Grafana               | Metrics collection & visualization         |
| **Security Scanning** | OWASP Dependency-Check             | CI/CD pipeline integration ✅               |
| **Deployment**        | Docker + Docker Compose            | Single server initially, K8s-ready         |
| **CI/CD**             | GitHub Actions                     | Automated test, build, scan, deploy        |
| **Geographic Scope**  | Single region initially            | Multi-region if needed later               |
| **Async Processing**  | **RabbitMQ**                       | Message broker, task queues, reliable delivery |


## 8.1 Complete Technology Stack
---

### Backend Framework & Build
---

| Component | Choice | Version | Notes |
|-----------|--------|---------|-------|
| Language | **Java** | 21 LTS | Latest long-term support |
| Framework | **Quarkus** | 3.x latest | Fast, lightweight, cloud-native |
| Build Tool | **Maven** | Latest | Standard for Java projects |
| Project Type | **Microservices** (Core API + specialized services) | - | Core API centralizes most business logic (users, orders, responses, reviews, reports, search); Auth, Messaging, Notification, and Moderation are separate services around it, each independently deployed (see [3 - System Design.md](3%20-%20System%20Design.md)) |

**Why Quarkus over Spring Boot?**
- ⚡ Faster startup (< 1 second)
- 💾 Lower memory footprint (perfect for containers)
- ☁️ Cloud-native, Kubernetes-ready
- 🚀 Great for MVP (less bloat than Spring)


### Database & Persistence
---

| Component | Choice | Version | Details |
|-----------|--------|---------|---------|
| Database | **PostgreSQL** | 14+ | Relational, robust, JSONB support |
| ORM | **Hibernate ORM** | Latest | Most popular with Quarkus |
| Connection Pooling | **HikariCP** | Auto | Built-in with Quarkus, optimized |
| Migrations | **Flyway** | Latest | Simple, version-based schema management |
| Validation | **Jakarta Bean Validation** | Latest | Built-in entity validation |


### Caching & Session Management
---

| Component | Choice | Strategy | Details |
|-----------|--------|----------|---------|
| Cache Layer | **Redis** | External service | In-memory, fast, scalable |
| Redis Client | **Jedis** | - | Thread-safe, simple integration with Quarkus |
| Cache TTL | - | 15-30 min (dynamic data) | Profiles, orders, searches |
| Session Storage | **Redis** | - | JWT tokens + Redis backup |


### API & Communication
---

| Component | Choice | Format | Details |
|-----------|--------|--------|---------|
| API Protocol | **REST** | JSON | HTTP-based, universal compatibility |
| Serialization | **Jackson** | JSON | Default with Quarkus, high performance |
| REST Framework | **JAX-RS** | Quarkus REST | Built-in, standard Jakarta EE |
| API Documentation | **OpenAPI/Swagger** | Interactive UI | Auto-generated docs at `/q/swagger-ui` |
| API Versioning | Current version only | N/A | No v1/v2 coexistence — pet project, no deprecation timeline needed |


### Security
---

| Component | Choice | Implementation | Details |
|-----------|--------|-----------------|---------|
| Authentication | **JWT** | Quarkus built-in | 15min access token, 7day refresh token |
| OAuth2/OIDC | **Quarkus OIDC** | Google OAuth integration | Built-in support for Google login |
| Password Hashing | **BCrypt** | Java security | Cost factor 12, industry standard |
| HTTPS/SSL | **Let's Encrypt** | Automatic | Free certificates for production |
| Rate Limiting | **Custom implementation** | Redis-based | 5 failed logins/15min, 1000 API calls/hour |
| Attack Prevention | All types | Best practices | SQL injection, XSS, DDoS. CSRF not applicable (Bearer tokens, no cookies) |
| Secrets Management | Environment variables | .env (dev) | Never commit to git |
| API Security Scanning | **OWASP Dependency-Check** | Maven plugin | Runs in GitHub Actions pipeline |


### Testing & Quality
---

| Component | Choice | Coverage | Details |
|-----------|--------|----------|---------|
| Unit Testing | **JUnit 5** | 80%+ | Modern standard for Java |
| Mocking | **Mockito** | - | Easy mock creation |
| Integration Testing | **Testcontainers** | All endpoints | Docker containers for databases |
| Test Coverage | **JaCoCo** | Report & gate | Minimum 80% required |
| API Testing | **REST Assured** | Fluent syntax | Clean, readable API tests |
| Code Quality | **Checkstyle** (future) | Phase 2+ | Code style enforcement |
| CI/CD | **GitHub Actions** | Automated | Test, build, scan, deploy |

**Test Execution:**
```text
1. Unit tests (fast, isolated)
2. Integration tests (with real DB via Testcontainers)
3. Code coverage check (80%+ required)
4. Security scanning (OWASP Dependency-Check)
```


### Logging & Monitoring
---

| Component | Choice | Format | Details |
|-----------|--------|--------|---------|
| Logging Framework | **SLF4J + Logback** | Structured | Standard for Java |
| Log Format | **JSON** | ElasticSearch | Integrated with ELK stack |
| Log Levels | DEBUG, INFO, WARN, ERROR | All types | Different detail levels |
| Log Storage | **ELK Stack** or **Loki** | Centralized | Search & analyze logs |
| Metrics Collection | **Micrometer** | - | Built-in with Quarkus |
| Metrics Server | **Prometheus** | - | Scrapes metrics every 15s |
| Dashboards | **Grafana** | Real-time | Visualize metrics & trends |
| Distributed Tracing | Not in MVP | Future Phase 2 | Can add Jaeger/Zipkin later |
| APM | Not in MVP | Future Phase 2 | DataDog/New Relic when needed |


### File Storage
---

| Component | Choice | Type | Details |
|-----------|--------|------|---------|
| Storage | **MinIO** or **AWS S3** | S3-compatible | For photos, portfolio images |
| Client Library | **MinIO Java SDK** | - | S3-compatible, easy migration |
| File Upload | **Quarkus REST multipart** | Built-in | Handle file uploads in API |
| Deployment | MinIO self-hosted (MVP) | S3 cloud (later) | Can migrate to AWS S3 anytime |

**What to store:**
- Professional profile photos
- Portfolio images
- Review evidence photos
- System backups (optional)


### Containerization & Deployment
---

| Component | Choice | Details |
|-----------|--------|---------|
| Container Image | **Docker** | Containerized from day 1 |
| Base Image | **Eclipse Temurin** | OpenJDK 21 LTS |
| JVM Mode | **Standard JVM** | Simpler than GraalVM native |
| Multi-stage Build | **Yes** | Optimized image size |
| Orchestration | **Docker Compose** (MVP) | Single server setup |
| K8s Ready | **Yes** | Prepared for Phase 2 scaling |
| Registry | **GitHub Container Registry** | Private image storage |

**Docker Setup:**
```text
Development: docker-compose up
Production: Single server with Docker
Scaling: Add Kubernetes in Phase 2
```


### Build & CI/CD Pipeline
---

| Stage | Tool | Action |
|-------|------|--------|
| **Trigger** | GitHub push | On commit to main/develop |
| **Test** | Maven Surefire | Run JUnit 5 + Mockito tests |
| **Integration Tests** | Maven + Testcontainers | Test all API endpoints |
| **Code Coverage** | JaCoCo | Verify 80%+ coverage |
| **Security Scan** | OWASP Dependency-Check | Scan for known vulnerabilities |
| **Build** | Maven | Compile + package JAR |
| **Docker Build** | Docker Compose | Build multi-stage image |
| **Deploy** | Manual trigger | Deploy to staging/production |

**Deployment Time:** < 5 minutes (target)


### Development Environment
---

| Tool | Purpose | Notes |
|------|---------|-------|
| **IDE** | IntelliJ IDEA Community | Best for Java development |
| **JDK** | Eclipse Temurin 21 LTS | Download & install |
| **Maven** | Build automation | Via IDE or command line |
| **Docker Desktop** | Local containerization | For testing containers locally |
| **Git** | Version control | GitHub repository |
| **Postman** | API testing | Optional manual testing |


### Dependency Management
---

| Category | Tool | Purpose |
|----------|------|---------|
| **Java Packages** | Maven Central | Public repository |
| **Quarkus Extensions** | Maven | Built-in Quarkus add-ons |
| **Dependency Scanning** | Maven plugin | Detect vulnerabilities |
| **Version Control** | pom.xml | Declarative dependency management |

**Quarkus Extensions Used (MVP):**
- quarkus-rest (REST API)
- quarkus-resteasy-jackson (JSON serialization)
- quarkus-hibernate-orm (ORM)
- quarkus-jdbc-postgresql (Database driver)
- quarkus-cache (Caching support)
- quarkus-redis-client (Redis integration)
- quarkus-jwt (JWT tokens)
- quarkus-oidc (Google OAuth)
- quarkus-security (Security features)
- quarkus-smallrye-health (Health checks)
- quarkus-smallrye-metrics (Prometheus metrics)
- quarkus-logging-json (JSON logging)
- quarkus-test-junit5 (Testing)
- quarkus-test-mockito (Mocking)


### Optional Future Additions
---

| Component | Purpose | Priority |
|-----------|---------|----------|
| Distributed Tracing | Jaeger/Zipkin | Low - for debugging if needed |
| APM | DataDog/New Relic | Low - advanced monitoring |
| Kubernetes | K8s orchestration | Medium - when multi-server needed |
| Vault | Secret management | Medium - enterprise security |
| Redis Cluster | High availability cache | Low - single instance OK initially |
| Database Replicas | Read-only replicas | Low - single DB OK initially |


## 8.2 Complete Tech Stack Summary
---

**Core Stack (Production Ready):**
- ✅ Java 21 + Quarkus 3.x + Maven
- ✅ PostgreSQL 14+ + Hibernate ORM + Flyway
- ✅ Redis (Jedis client)
- ✅ Elasticsearch 8.x
- ✅ MinIO (S3-compatible file storage)
- ✅ **RabbitMQ 3.12+** (message broker, AMQP protocol)
- ✅ OpenAPI/Swagger documentation
- ✅ Quarkus Security (JWT + OIDC)
- ✅ JUnit 5 + Mockito + Testcontainers (80%+ coverage)
- ✅ SLF4J + Logback + JSON (structured logging)
- ✅ Micrometer + Prometheus + Grafana (monitoring)
- ✅ ELK Stack or Loki (log aggregation)
- ✅ Docker + Docker Compose (containerization)
- ✅ GitHub Actions (CI/CD with OWASP security scanning)

**Total Dependencies:** ~33-38 Maven artifacts (managed by Quarkus Bill of Materials + RabbitMQ client)


## 9. Stakeholders
---

| Stakeholder | Interest | Priority |
|------------|----------|----------|
| Customers | Find trusted professionals quickly | High |
| Professionals | Access to jobs and reputation building | High |
| Admin/Moderator | Platform quality and safety | High |
| Platform Owner | Revenue and scalability | Medium |


## 10. Success Metrics (Phase 1)
---

- [ ] 1000+ registered professionals
- [ ] 5000+ registered customers
- [ ] 100+ active orders per week
- [ ] 4.5+ average professional rating
- [ ] < 1% dispute rate
- [ ] 99.5% platform uptime


## 11. Launch Acceptance Criteria
---

Before production launch, system must pass:

**Functional:**
- [ ] All 55+ functional requirements implemented
- [ ] All 10 use cases working end-to-end
- [ ] Search with Elasticsearch working (all 7 filters + full-text)
- [ ] Messaging system working (text + file sharing)
- [ ] Review system functional (post, read, reply, moderation)
- [ ] Moderator admin panel complete
- [ ] Notification system working (push, email, in-app)
- [ ] Async task processing working (Message Queue)

**Non-Functional:**
- [ ] Unit tests: 80%+ coverage
- [ ] Integration tests: All API endpoints tested
- [ ] API response time: < 1.5s (95th percentile)
- [ ] Docker Compose deployment fully working
- [ ] Logging configured (ELK or Loki with retention)
- [ ] Monitoring & alerting (Prometheus + Grafana)
- [ ] Complete API documentation (OpenAPI/Swagger)
- [ ] CI/CD pipeline automated (GitHub Actions)

**Security:**
- [ ] Password hashing (bcrypt, cost factor 12)
- [ ] JWT authentication & token rotation working
- [ ] HTTPS/SSL in production (Let's Encrypt)
- [ ] Rate limiting enforced
- [ ] SQL injection prevention tested & verified
- [ ] XSS prevention tested & verified
- [ ] CORS allowlist enforced (no `*`); CSRF N/A (Bearer tokens, no cookies)
- [ ] Refresh-token rotation + reuse detection working; Redis denylist revocation working
- [ ] OAuth2 (Google) working

**Performance:**
- [ ] Redis caching working (sessions, profiles)
- [ ] Elasticsearch indexes built & optimized
- [ ] Database queries optimized (no N+1 queries)
- [ ] Search results < 500ms
- [ ] API response time for core operations < 200ms
- [ ] File uploads working with MinIO


## 12. Key Decisions Made
---

| Question | Decision | Status |
|----------|----------|--------|
| Payments? | Free marketplace (no payments) | ✅ Decided |
| Order statuses? | 3 statuses (DRAFT, ACTIVE, CLOSED) — no delete, no reopening | ✅ Decided |
| Monetization? | Ideas for later (premium, ads, subscriptions); **nothing in MVP**, no paid ranking | ✅ Decided |
| Professional verification? | Not implemented — removed from scope entirely | ✅ Decided |
| 2FA? | Removed from MVP (2026-08-27) — Phase 2 | ✅ Decided |
| Account deletion? | Soft delete + PII scrub + 30-day grace (added 2026-08-27) | ✅ Decided |
| Professional responses? | One editable response per professional per order; sealed bids; no withdrawal | ✅ Decided |
| Search default sort? | "Recommended" = Bayesian-adjusted rating; no featured/promoted | ✅ Decided |
| Reliable events? | Transactional outbox → RabbitMQ; idempotent consumers | ✅ Decided |
| Detailed business-logic questions? | All resolved and folded into [4 - Business logic.md](4%20-%20Business%20logic.md) | ✅ Decided |
| Chat without an order? | Customer can message any professional anytime, not gated by a response; conversation keyed by (customer, professional) pair | ✅ Decided (2026-09-04) |
| Category hierarchy? | Flat list, no parent/leaf tree | ✅ Decided (2026-09-04) |
| Order currency? | Single platform currency: USD | ✅ Decided (2026-09-04) |
| `preferred professional level` field? | Dropped — leftover from the removed professional-verification concept | ✅ Decided (2026-09-04) |
| `latitude`/`longitude` on order? | Dropped — search is city-only, fields served no purpose | ✅ Decided (2026-09-04) |
| Time zones? | No per-user timezone support in MVP; can be added later | ✅ Decided (deferred, 2026-09-04) |
| Alerts? | Automated alerts via Prometheus/Grafana from day 1 | ✅ Decided |
| Async processing? | RabbitMQ/Kafka for notifications, emails, background jobs | ✅ Decided |
| Search? | Elasticsearch integration for fast filtering | ✅ Decided |
| Backend language? | Java 21 + Quarkus 3.x | ✅ Decided |
| Uptime requirement? | 95% acceptable initially | ✅ Decided |
| Testing coverage? | 80%+ unit + all integration tests | ✅ Decided |
| Deployment? | Docker + Docker Compose (single server, K8s-ready) | ✅ Decided |
| Architecture? | Microservices: Core API (central, most business logic) + Auth/Messaging/Notification/Moderation services + Message Queue — **not** a monolith | ✅ Decided (clarified 2026-09-04) |


## Summary: Complete Feature Set
---

✅ **55+ Functional Requirements** - all features defined
✅ **Complete NFR Matrix** - performance, security, scalability, maintainability
✅ **10 Key Use Cases** - main user flows documented
✅ **Full Stack Included:**
  - Authentication (email + Google OAuth)
  - Core marketplace (orders, responses, reviews)
  - Real-time messaging with file sharing
  - Advanced search (Elasticsearch)
  - Notification system (push, email, in-app)
  - Async task processing (RabbitMQ/Kafka)
  - Reactive content moderation (reports-driven)
  - File storage (MinIO)

✅ **Security** - bcrypt, JWT, rate limiting, all attack prevention
✅ **Testing** - 80%+ coverage + all integration tests
✅ **Monitoring** - Prometheus + Grafana + ELK/Loki
✅ **Deployment** - Docker Compose ready, Kubernetes-ready


## Changelog
---

| Version | Date | Change |
|---|---|---|
| 1.6 | 2026-09-10 | Standardised to the shared doc format (title, metadata, separators, changelog). Renumbered the duplicated `## 9` / `## 10` (Launch Acceptance Criteria → §11, Key Decisions Made → §12). Fixed the test/lint stack in NFR-Maint-2 / NFR-Maint-7 (Jest/Pytest/ESLint/Prettier → JUnit 5 + JaCoCo + Checkstyle + Spotless, matching § Tech Stack and [15 - SDLC & Workflow.md](15%20-%20SDLC%20%26%20Workflow.md)). Repointed the dead file-5 links (a "Questions" doc that no longer exists — slot 5 is now State Machines) to [4 - Business logic.md](4%20-%20Business%20logic.md). Removed the stale "Next Steps" section. |
| 1.5 | 2026-09-05 | Confirmed load balancer is used from day 1 — fixed NFR-Scale-1/2, which previously said load balancing was "not needed", contradicting [3 - System Design.md](3%20-%20System%20Design.md)'s Nginx Gateway. |
| 1.4 | 2026-09-04 | Fixed the § 8.1 project-type contradiction: was "Modular Monolith", corrected to microservices, matching [3 - System Design.md](3%20-%20System%20Design.md) and [7 - Application Classes.md](7%20-%20Application%20Classes.md). |
| 1.3 | 2026-09-04 | Closed Documentation Roadmap Block A gaps A1/A4/A5: chat decoupled from order response, categories flattened, currency = USD, dropped `preferred professional level` and order `latitude`/`longitude`. |
| 1.2 | 2026-08-27 | Synced with all resolved business-logic questions (now in [4 - Business logic.md](4%20-%20Business%20logic.md)): 2FA removed, account deletion added, one-response model, search ranking, transactional outbox, session/rate-limit/file/notification/moderation detail. |
| 1.1 | 2026-08-11 | Unified MVP + Phase 2 into a single launch scope. |
| 1.0 | 2026-08-06 | **Approved.** Final comprehensive MVP requirements. |
| 0.3 | 2026-08-06 | NFR questionnaire responses integrated. |
| 0.2 | 2026-08-05 | Added monetization model & simplified order statuses. |
| 0.1 | 2026-08-05 | Initial requirements gathering. |
