# ProFinder — Documentation Roadmap

- **Version:** 3.0
- **Date:** 2026-09-10
- **Status:** Living
- **Purpose:** Track everything still needed to make the project fully documented before implementation.

This file complements [tasks.md](tasks.md) (the original 57-item design-task checklist). The detailed
business-logic questions that used to live in a separate "5 - Questions for Complete Business Logic"
doc are all resolved and folded into [2 - Requirements.md](2%20-%20Requirements.md) and
[4 - Business logic.md](4%20-%20Business%20logic.md); slot 5 is now [5 - State Machines.md](5%20-%20State%20Machines.md).
This roadmap focuses on **what is genuinely missing or undecided**, in priority order.

**Current documents:**
- [1 - Description.md](1%20-%20Description.md) — overview ✅ (v1.1)
- [2 - Requirements.md](2%20-%20Requirements.md) — FR / NFR / use cases ✅ (v1.6)
- [3 - System Design.md](3%20-%20System%20Design.md) — architecture ✅ (v1.4 — RabbitMQ routing reconciled w/ file 9; JWT validation = "local + Redis denylist")
- [4 - Business logic.md](4%20-%20Business%20logic.md) — business rules ✅ (v3.6)
- [5 - State Machines.md](5%20-%20State%20Machines.md) — Mermaid state diagrams for Order, Professional Profile, Account, Report, Ban Appeal, Review ✅ (v1.1)
- [6 - Database Schema.md](6%20-%20Database%20Schema.md) — full table breakdown + domain-grouped ER diagrams + enums + indexes ✅ (v1.3 — token tables added)
- [7 - Application Classes.md](7%20-%20Application%20Classes.md) — Entity/Repository/Service/Controller/DTO breakdown per microservice ✅ (v1.1)
- [8 - API Specification.md](8%20-%20API%20Specification.md) — conventions + Error Catalog + all 5 services' endpoints ✅ (v1.8 — `DELETE /me` added)
- [9 - Event Catalog.md](9%20-%20Event%20Catalog.md) — envelope + queue topology + retry/DLQ + 29 events w/ payload schemas + cascade map ✅ (v1.3)
- [10 - Sequence Diagrams.md](10%20-%20Sequence%20Diagrams.md) — 14 Mermaid `sequenceDiagram` flows ✅ (v1.1)
- [11 - Permission Matrix.md](11%20-%20Permission%20Matrix.md) — role × action matrix, account-status gate, STANDARD/SENIOR split, enforcement ✅ (v1.1)
- [12 - Elasticsearch Mapping.md](12%20-%20Elasticsearch%20Mapping.md) — `professionals` + `orders` index mappings, analyzers, document assembly, query shapes ✅ (v1.1)
- [13 - Validation Rules.md](13%20-%20Validation%20Rules.md) — per-field constraint tables for every API input; all 6 gaps resolved ✅ (v1.2)
- [14 - Category Taxonomy.md](14%20-%20Category%20Taxonomy.md) — 32 seed categories + Flyway INSERT + rename/delete notes ✅ (v1.1)
- [15 - SDLC & Workflow.md](15%20-%20SDLC%20%26%20Workflow.md) — repo layout, issue→prod workflow, environments, git branching, CI/CD, testing strategy ✅ (v1.1) — **partially delivers Tier 3**

---

## BLOCK A — Open business-logic gaps (decide before DB schema)
---

### A1. Chat / conversation model — ✅ Decided (2026-09-04)
---

Chat can be started **without an order**. Customer can message any professional at any time — from a search result / profile, or after a response. Not gated by an order response. Conversation is keyed by `(customer, professional)` pair, not by order. Folded into [2 - Requirements.md](2%20-%20Requirements.md) (FR-Communication-1) and [4 - Business logic.md](4%20-%20Business%20logic.md) (new "Conversation Model" subsection).

### A2. Professional → customer reviews ("customer reliability rating") — ✅ Decided (2026-09-04)
---

Mirrors the customer→professional model: one editable 1–5 (+ optional text) review per professional-customer pair, postable anytime, re-submitting edits it. Customer **cannot reply** in MVP — may be added later. Folded into [2 - Requirements.md](2%20-%20Requirements.md) (FR-Profile-C4) and [4 - Business logic.md](4%20-%20Business%20logic.md) (Customer Reliability Rating).

### A3. Admin vs Moderator role — ✅ Decided (2026-09-04)
---

No separate Admin role. Admin is just a Moderator with the top permission level (FR-Moderator-9) — a fine-grained permission system is deferred to a later phase, not needed now. Folded into [2 - Requirements.md](2%20-%20Requirements.md) (FR-Auth-3, FR-Admin note) and [4 - Business logic.md](4%20-%20Business%20logic.md) (Moderator role permissions note).

### A4. Order ↔ professional category matching — ✅ Decided (2026-09-04)
---

Categories are simplified to a **flat list — no hierarchy**. An order's `category_id` matches a professional only on exact category equality; the parent/leaf matching question no longer applies. Folded into [4 - Business logic.md](4%20-%20Business%20logic.md) (Service Categories, Search Filters) and [2 - Requirements.md](2%20-%20Requirements.md) (FR-Search-1).

### A5. Small undefined fields — ✅ Decided (2026-09-04)
---

- **`preferred professional level`** — dropped. Leftover from the removed professional-verification concept, never had a defined meaning.
- **Currency** — single platform currency: **USD**. No payments exist yet, so this only matters for display consistency.
- **Time zones** — no per-user timezone support in MVP; plain timestamps, no conversion. Can be revisited later if needed.
- **`latitude` / `longitude` on the order** — dropped. Search is city-only (no radius), so the fields served no purpose.


## Architecture question — ✅ Resolved (2026-09-04)
---

[2 - Requirements.md § 8.1](2%20-%20Requirements.md) said **Project Type: Modular Monolith**, contradicting [3 - System Design.md](3%20-%20System%20Design.md)'s 5 independently deployed services. **Decided: genuine microservices, not a monolith.** Core API is the central service and handles most business logic (users, orders, responses, reviews, reports, search); Auth, Messaging, Notification, and Moderation are separate services around it, each for a specific concern (security isolation, WebSocket, async delivery, content checks). Fixed in [2 - Requirements.md](2%20-%20Requirements.md) v1.4 § 8.1 and Key Decisions table. [7 - Application Classes.md](7%20-%20Application%20Classes.md) already matched this (built from file 3), so no rework needed there.


## BLOCK B — Engineering documents to produce
---

### Tier 1 — blocks writing any real code
---

- [x] **Database Schema / ERD** — [6 - Database Schema.md](6%20-%20Database%20Schema.md). Done 2026-09-04.
- [x] **API Specification** — [8 - API Specification.md](8%20-%20API%20Specification.md), all 5 services. Written as readable Markdown, not a raw OpenAPI YAML file ([user's choice](Documentation%20Roadmap.md), 2026-09-05) — Quarkus's `quarkus-smallrye-openapi` will auto-generate the real machine-readable spec from code once it exists. Done 2026-09-05.
- [x] **Error Catalog** — merged into [8 - API Specification.md](8%20-%20API%20Specification.md) (a standalone file made no sense once endpoints reference it). Done 2026-09-05.
- [x] **Event Catalog + payload schemas** — [9 - Event Catalog.md](9%20-%20Event%20Catalog.md), done 2026-09-07. Standard envelope, `profinder.events` topic exchange + queue topology, retry/DLQ policy, 29 events (routing key, producer, trigger, consumers, payload schema, idempotency), cascade map. All 10 decisions resolved (file 9 §8). **Reconcile complete:** file 3 and file 4 both match the catalog; the JWT local-vs-HTTP validation inconsistency is also resolved (file 3 v1.3 → local verify + Redis denylist, matching files 4 & 10).

### Tier 2 — before implementing the matching module
---

- [x] **Auth flows** — [10 - Sequence Diagrams.md § A](10%20-%20Sequence%20Diagrams.md) (register/verify, login+lockout, per-request authz, refresh+rotation+reuse, Google OAuth+role, password reset) + JWT claims (`sub, exp, role, account_status`) + **permission matrix** now in [11 - Permission Matrix.md](11%20-%20Permission%20Matrix.md). Done 2026-09-07.
- [x] **Elasticsearch mapping** — [12 - Elasticsearch Mapping.md](12%20-%20Elasticsearch%20Mapping.md): `professionals` + `orders` mappings, `text_en`/`name_analyzer`/`city_normalizer`, document-assembly pseudocode per `search.*_reindex` consumer, query shape per sort mode. Done 2026-09-07.
- [x] **Category taxonomy** — [14 - Category Taxonomy.md](14%20-%20Category%20Taxonomy.md): 32 seed categories + Flyway INSERT + rename/delete semantics. Done 2026-09-07.
- [x] **Validation rules** — [13 - Validation Rules.md](13%20-%20Validation%20Rules.md): per-field constraint tables for all API inputs, global rules, file-upload rules; all 6 spec gaps resolved (v1.1). Done 2026-09-07.
- [x] **State machines** — [5 - State Machines.md](5%20-%20State%20Machines.md). Done 2026-09-04.
- [x] **Messaging Service detail** — [8 - API Specification.md § Messaging Service](8%20-%20API%20Specification.md). WebSocket-over-LB solved via Redis Pub/Sub (already implied by [3 - System Design.md](3%20-%20System%20Design.md)'s "WebSocket state" Redis use case), message envelope, auth handshake, delivery receipts, reconnection fallback all specified. Done 2026-09-05.
- [x] **Module / package structure** — [7 - Application Classes.md](7%20-%20Application%20Classes.md) gives the per-service class breakdown (microservices, not a monolith — see architecture question above, resolved). Done 2026-09-04.
- [x] **API conventions** — [8 - API Specification.md § Conventions](8%20-%20API%20Specification.md) v1.7 now also fixes **UUID = v7** (time-ordered, app-generated) and **enum i18n** (API returns raw tokens, client localizes via `enum.<name>.<value>` bundle keys). Nothing left open here.

### Tier 3 — operational, before launch (not before first code)
---

- [ ] **CONFIGURATION.md** — every admin-configurable knob (`*_ttl`, `page_size_*`, `deletion_grace_days`, `bayes_prior_*`, rate-limit caps, file-size limits, retention windows, …).
- [ ] **Flyway migrations plan** — V001, V002, … ordering (includes `email_verification_tokens` / `password_reset_tokens`, the 32-row category seed, enums).
- [ ] **Seed / test data & fixtures.**
- [~] **docker-compose** for local dev — outline in [15 - SDLC & Workflow.md § 3](15%20-%20SDLC%20%26%20Workflow.md); still needs the actual `docker-compose.yml`.
- [~] **CI/CD pipeline** — workflow shape (test / quality / deploy) in [15 - SDLC & Workflow.md § 5](15%20-%20SDLC%20%26%20Workflow.md); still needs the real `.github/workflows/*.yml`.
- [ ] **Deployment guide** (VPS, single server) + a deployment diagram (Block C).
- [ ] **Monitoring dashboards** spec, **logging format** spec, **alerting rules**.
- [~] **Testing strategy** — covered in [15 - SDLC & Workflow.md § 6](15%20-%20SDLC%20%26%20Workflow.md); **load testing plan** still open.
- [ ] **Developer guide**, **ADRs**, **privacy / ToS** pages.


## BLOCK C — UML diagrams
---

Format: **Mermaid** as primary (text, diffable, renders in GitHub/VS Code/Obsidian/Notion; I can author it).
PlantUML only if strict UML notation is needed. Prose fallback where a diagram is too large for Mermaid.

These are **not 6 new documents** — they are visualizations embedded in the docs already planned above.
Only **Sequence** warrants its own file.

| UML type | Concrete diagrams for ProFinder | Lives in | Mermaid | Priority |
|---|---|---|---|---|
| **State** | order; professional profile; account; report; review moderation; ban appeal | [State Machines.md](5%20-%20State%20Machines.md) ✅ done | `stateDiagram-v2` | high — done 2026-09-04 |
| **Sequence** | ✅ **done** — [10 - Sequence Diagrams.md](10%20-%20Sequence%20Diagrams.md), 14 diagrams (auth ×6, orders ×2, WebSocket chat, review→reindex, search, upload, report→ban cascade, account deletion) | [10 - Sequence Diagrams.md](10%20-%20Sequence%20Diagrams.md) | `sequenceDiagram` | high — done 2026-09-07 |
| **Component** | services + infra + edges (what file 3 draws in ASCII) | System Design | `flowchart` / C4-style | medium — file 3 covers it, cleaner diagram helps |
| **Class** | ~~domain model: aggregates, entities, relationships, invariants~~ — superseded by [7 - Application Classes.md](7%20-%20Application%20Classes.md) ✅ done | [7 - Application Classes.md](7%20-%20Application%20Classes.md) | table, not `classDiagram` (user's choice) | done 2026-09-04 — full MVC breakdown instead of an aggregates-only diagram |
| **Deployment** | nodes (VPS / Docker containers / external services), ports, networks | Deployment Guide | `flowchart` | medium — one diagram in the deploy doc |
| **Activity** | end-to-end journeys: customer path (find → order → chat → close → review), professional onboarding, dispute resolution | "User Flows" doc (already in tasks.md) | `flowchart` | medium |


## Proposed order of work
---

1. ✅ **Block A** — close the 5 business-logic gaps (A1–A5), fold into files 2 & 4. Done 2026-09-04.
2. ✅ **State diagrams** — [5 - State Machines.md](5%20-%20State%20Machines.md). Done 2026-09-04.
3. ✅ **Database Schema / ERD** — [6 - Database Schema.md](6%20-%20Database%20Schema.md). Domain Class diagram replaced by [7 - Application Classes.md](7%20-%20Application%20Classes.md) (full MVC breakdown per service, by user's request) instead of a separate aggregates-only diagram. Done 2026-09-04.
4. ✅ **API Specification** + **Error Catalog** (both in [8 - API Specification.md](8%20-%20API%20Specification.md)) + **Event Catalog** ([9 - Event Catalog.md](9%20-%20Event%20Catalog.md), all decisions resolved).
5. ✅ **Sequence diagrams** — [10 - Sequence Diagrams.md](10%20-%20Sequence%20Diagrams.md), 14 flows. Also closed the JWT local-vs-HTTP inconsistency (file 3 → v1.3).
6. ✅ **Permission matrix ([11](11%20-%20Permission%20Matrix.md)), ES mapping ([12](12%20-%20Elasticsearch%20Mapping.md)), Validation rules ([13](13%20-%20Validation%20Rules.md)), Category seed set ([14](14%20-%20Category%20Taxonomy.md)), UUID + enum-i18n conventions.** **Tier 1 & Tier 2 are complete — nothing blocks writing code.**
7. ✅ **Doc consistency + standard formatting pass** (2026-09-10) — all files onto one house style; broken links, numbering, counts, stale notes and a set of cross-file conflicts fixed (see progress log).
8. **Component + Deployment + Activity** diagrams — into System Design / Deployment Guide / User Flows. Optional polish, not a code blocker.
9. **Operational docs (Tier 3)** — [15 - SDLC & Workflow.md](15%20-%20SDLC%20%26%20Workflow.md) covers the workflow/CI/testing shape; still open: CONFIGURATION.md, the real migrations plan, `docker-compose.yml`, the real GitHub Actions workflows, monitoring/logging/alerting specs, deployment guide, load-testing plan, developer guide, ADRs, privacy/ToS. Build-time-parallel phase.


## Progress log
---

| Date | Done |
| ---- | ---- |
| 2026-09-10 | **Doc consistency + single standard format pass** across all 15 numbered files + this roadmap + [tasks.md](tasks.md). Applied the house style (title, `**Version/Date/Status/Purpose**` block, `---` under every heading, `## Changelog` table). Fixes: repointed 3 dead "5 - Questions for Complete Business Logic" links → files 2 & 4; renumbered duplicate `## 9`/`## 10` in file 2 and the missing `## 8` in file 9 (+ its broken anchor links); category count reconciled to **32** (was 30/31/32); cleared stale "not-yet-written / deferred / to-be-corrected" notes in files 6/7/8/10. Substantive: **added `email_verification_tokens` + `password_reset_tokens`** to file 6; **added `DELETE /me`** to file 8 and `AccountService` to file 7; `ACCOUNT_DELETION_BLOCKED` unified to **409** across files 8/10/13; fixed the JS/Python test-stack leftovers in file 2 NFR-Maint-2/7; fenced file 3's Phase-2 Moderation detail. Added file 15 to this roadmap; annotated Tier 3 with what file 15 delivers. |
| 2026-09-07 PM | **VALIDATION & SCHEMA FINALIZED.** [13 - Validation Rules.md](13%20-%20Validation%20Rules.md) → v1.1: **all 6 gaps resolved** (①flat 0–70 cap for `years_experience`, ②–⑧text-length limits pinned, ③`display_name` character class, ④pagination clamp, ⑤ignore unknown JSON, ⑥`preferred_date` = today+). [6 - Database Schema.md](6%20-%20Database%20Schema.md) → v1.2: **all constraints added to DDL** (string lengths, CHECK on years_exp/rating/money fields, `ban_appeals.text` column added). **TIER 2 100% COMPLETE.** Ready for code: database schema is finalized; validation rules are specified; all business logic is documented; all APIs and events are catalogued. **Next: start Flyway migrations + begin Phase 1 code sprint.** |
| 2026-09-07 | **Tier 2 closed.** Created [11 - Permission Matrix.md](11%20-%20Permission%20Matrix.md) (role × action across all 5 services, account-status gate, STANDARD/SENIOR = the only two-tier split, `moderator_level`/`profile_status` read from DB not JWT), [12 - Elasticsearch Mapping.md](12%20-%20Elasticsearch%20Mapping.md) (`professionals` + `orders` mappings, `text_en`/`name_analyzer`/`city_normalizer`, `sort_rating`/`popularity` precomputed at index time, full document-assembly pseudocode per `search.*_reindex` consumer, query shape per sort mode, alias/reindex ops), [13 - Validation Rules.md](13%20-%20Validation%20Rules.md) (per-field tables for every input; 6 gaps surfaced), [14 - Category Taxonomy.md](14%20-%20Category%20Taxonomy.md) (32 seed categories + Flyway INSERT + rename→reindex consequence). File 8 → v1.7: UUID v7 + enums i18n. |
| 2026-09-04 | Closed all of Block A (A1–A5): chat decoupled from order, categories flattened, currency = USD, dropped `preferred professional level` and order lat/long, customer-reliability review mechanics defined, Admin confirmed as top-permission Moderator (no 4th role). |
| 2026-09-04 | Created [State Machines.md](5%20-%20State%20Machines.md): Mermaid diagrams for Order, Professional Profile, Account (consolidated from 3 scattered sections), Report, Ban Appeal, Review moderation. |
| 2026-09-04 | Closed the 2 gaps found while diagramming: professional can't drop below 1 category/1 city (replace-only, `ACTIVE` is sticky); `PENDING_VERIFICATION` accounts can't be suspended/banned (must verify first). Folded into [4 - Business logic.md](4%20-%20Business%20logic.md) v3.4. |
| 2026-09-04 | Created [6 - Database Schema.md](6%20-%20Database%20Schema.md): overview ERD + 8 domain-grouped ERDs, full column tables, 13 enums, key indexes. Added 4 tables beyond the original checklist (`professional_categories`, `professional_cities`, `ban_appeals`, unified `files`). Reworked v1.1: moved columns out of Mermaid into plain markdown tables, diagrams now relationships-only. |
| 2026-09-04 | Created [7 - Application Classes.md](7%20-%20Application%20Classes.md) instead of a domain-only Class diagram: full Entity/Repository/Service/Controller/DTO breakdown per microservice (Core API, Auth, Messaging, Notification, Moderation) + a Shared/Cross-Service section. Flagged one inconsistency between files 3 and 4 on JWT validation (local vs HTTP) — not fixed, just noted. |
| 2026-09-04 | Resolved the "Modular Monolith" vs microservices contradiction: confirmed genuine microservices, Core API central + Auth/Messaging/Notification/Moderation as specialized services around it. Fixed [2 - Requirements.md](2%20-%20Requirements.md) v1.4 § 8.1 and Key Decisions table to match [3 - System Design.md](3%20-%20System%20Design.md). |
| 2026-09-05 | Created (then merged) [8 - API Specification.md](8%20-%20API%20Specification.md): API conventions (offset pagination, error envelope, USD, ISO dates) + full Error Catalog (~30 codes across 9 domains + infra-failure mapping), written as Markdown not raw OpenAPI YAML per the user's choice. |
| 2026-09-05 | Confirmed load balancer used from day 1 (user decision) — fixed [2 - Requirements.md](2%20-%20Requirements.md) v1.5 NFR-Scale-1/2, which contradicted [3 - System Design.md](3%20-%20System%20Design.md)'s Nginx Gateway. Wrote Core API endpoints in [8 - API Specification.md](8%20-%20API%20Specification.md) v1.2 (Orders, Responses, Reviews, Profiles & Categories, Reports & Moderation, Search, Files, Health) with LB implications noted (statelessness, health checks, WebSocket flagged for later). |
| 2026-09-05 | Added `/api/v1` base-path convention (user's suggestion) to [8 - API Specification.md](8%20-%20API%20Specification.md). Wrote Auth Service endpoints (v1.3): registration/verification, login/OAuth, sessions/tokens, health. Noted login succeeds even for suspended/banned users (they need to reach the appeal/support screen) and always returns 202 on forgot-password regardless of whether the email exists (anti-enumeration). |
| 2026-09-05 | Found the WebSocket-over-LB answer already implied in [3 - System Design.md](3%20-%20System%20Design.md) (Redis "WebSocket state" use case) — made it explicit: Redis Pub/Sub per-user channel, not LB sticky sessions, which also gives multi-device support for free. Wrote Messaging Service (v1.4): WS protocol/envelope, REST history/fallback endpoints, health. |
| 2026-09-05 | Wrote Notification Service and Moderation Service endpoints (v1.5) — **API Specification is now complete for all 5 services.** Moderation Service's sync-check endpoints marked internal-only (network trust, no end-user JWT); its async/ML pipeline stays flagged Phase 2. Only Event Catalog remains open in Tier 1, still deferred per the user's request. |
| 2026-09-05 | Fixed a structural bug (user caught it): Auth/Messaging/Notification/Moderation Service sections had ended up nested under `## Error Catalog` instead of `## Endpoints`, because each was inserted before `## Next` without checking what came immediately before it. Rebuilt [8 - API Specification.md](8%20-%20API%20Specification.md) (v1.6) with all 5 services correctly under `## Endpoints`, `## Error Catalog` as its own sibling section after. |
| 2026-09-07 | Created [9 - Event Catalog.md](9%20-%20Event%20Catalog.md) v1.0: transactional-outbox transport model, standard event envelope, `profinder.events` topic exchange + queue topology, retry/DLQ policy per queue class, 27 events fully specified (triggers, consumers, payloads), notification⇄reindex cascade map. Surfaced 4 routing-key bugs in file 3 (plural `orders.*` bindings never match singular `order.published`; `search.*` doesn't match `professional.reindex`; `user.blocked` has no queue; stale `email.queue`/"Email Worker" vs file 7's inline email). |
| 2026-09-07 | file 9 v1.1 — resolved all 10 decisions. **User's calls:** drop `order.draft_created` entirely; `response.edited` in-app only; **no** moderator alert on new reports (polling stays); `message.deleted` **is** in MVP (users can delete own messages); **Auth Service emails route through Notification Service** (Auth becomes a 2nd event producer — `auth.verification_requested`/`auth.password_reset_requested`/`auth.password_set_requested`, new `auth.notifications` queue, Auth gets its own outbox). **Defaults taken:** named `profinder.events` exchange, `account.deletion_finalized` added, no TTL on `search.index`, one shared DLQ, 7-day outbox retention, all events `v1`. Event count 27→29. Added inconsistency I5. |
| 2026-09-07 | Created [10 - Sequence Diagrams.md](10%20-%20Sequence%20Diagrams.md) v1.0 — 14 Mermaid `sequenceDiagram` flows: A. auth (register+verify, login+lockout, per-request local JWT authz + Redis denylist, refresh w/ rotation+reuse detection, Google OAuth+role selection, password reset); B. publish order → outbox → notify+reindex, professional responds (FOR UPDATE race); C. WebSocket chat over Redis Pub/Sub w/ offline→notification fallback; D. review → sync moderation → sync rating recalc → cache delete → reindex, search cache→ES, file upload pipeline (magic bytes / EXIF strip / re-encode / MinIO); E. report → moderator ban → transactional cascade (auto-close orders, denylist, reindex, notify), account deletion → 30-day grace → finalize. **Fixed the last cross-file inconsistency:** file 3 → v1.3, "Core API ↔ Auth Service" now says JWT validation is local (signature + Redis denylist), not a per-request HTTP call — matches file 4. |
| 2026-09-07 | Reconcile pass for file 9. **file 3 → v1.2:** routing keys now singular `<aggregate>.<event>` with `<aggregate>.*` bindings (old plural `orders.*` never matched `order.published`); reindex events `search.professional_reindex`/`search.order_reindex`; `user.*` + `auth.*` bindings + `auth.notifications` queue added; `email.queue`/"Email Worker" removed (Notification Service sends email inline); Auth & Messaging Services documented as event producers; exchange `profinder.events`. **file 4 → v3.5:** Events section rewritten to match the catalog (drop `order.draft_created`, split into `response.created`/`response.edited`, add `message.sent`/`message.deleted`/`report.created`/`account.deletion_finalized`/`auth.*`, rename reindex events, search-index queue = unlimited retry no DLQ). file 9 → v1.2. |
| 2026-09-05 | Housekeeping pass on this roadmap itself (v2.0): synced stale document version numbers, marked "Proposed order of work" steps done, updated the BLOCK C Class-diagram row to point at [7 - Application Classes.md](7%20-%20Application%20Classes.md). |
