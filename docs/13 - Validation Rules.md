# ProFinder — Validation Rules

- **Version:** 1.2
- **Date:** 2026-09-10
- **Status:** Stable
- **Purpose:** One consolidated table of every API input field — type, whether it's required, its constraints (length / range / format / allowed values), and the error it raises. [Documentation Roadmap.md](Documentation%20Roadmap.md) Tier 2 "Validation rules".

**Mechanism:** Jakarta Bean Validation ([2 § Tech Stack](2%20-%20Requirements.md)) on request DTOs. A failure returns `400 VALIDATION_ERROR` with `details` as a `{field: reason}` map ([8 § Response Envelope](8%20-%20API%20Specification.md)). Domain rules that need a DB lookup (uniqueness, existence, cross-row invariants) are checked in the service layer and may raise a domain-specific code.

> All 6 spec gaps identified in v1.0 were resolved in v1.1 (see §12).

---

## 1. Global rules
---

Applied everywhere, not repeated per field below.

| Rule | Detail |
|---|---|
| **Trim** | All string fields are trimmed of leading/trailing whitespace before validation and storage. |
| **Empty = null** | An empty string after trim is treated as `null` (and then fails `@NotBlank` if required). |
| **Control chars** | Reject `U+0000`–`U+001F` and `U+007F` except `\n` / `\t` inside multi-line text fields (`bio`, `description`, review `text`, message `text`, report `description`, `notes`). |
| **UUID path/body params** | Must match the UUID format before any DB lookup; a malformed id → `400 VALIDATION_ERROR`, never `404`. |
| **Unknown JSON fields** | Ignored (Jackson `FAIL_ON_UNKNOWN_PROPERTIES = false`) — gap ⑤. |
| **Numbers** | `NaN` / `Infinity` rejected. Money fields: max 2 decimal places, non-negative. |
| **Timestamps in requests** | ISO-8601; no per-user timezone handling ([A5](Documentation%20Roadmap.md)). |
| **Pagination** | `page` ≥ 1 (1-based), default 1. `size` 1–100, default 20 (`page_size_max` config). Out-of-range → **clamped**, not an error (④). |
| **String length** | Counted in Unicode code points, not bytes or UTF-16 units. |


## 2. Auth & account
---

| Field | Endpoint(s) | Required | Constraints | Error |
|---|---|---|---|---|
| `email` | register, login, forgot-password | ✅ | `@Email`; ≤ 254 chars; normalized lowercase; case-insensitive unique (register). Disposable-domain blocklist (config, empty in MVP). No MX check. | `VALIDATION_ERROR`; `EMAIL_ALREADY_REGISTERED` (register) |
| `password` | register, reset-password | ✅ | ≥ `password_min_length` (default **8**); complexity: ≥1 upper, ≥1 lower, ≥1 digit, ≥1 special — each toggle config, all on by default; ≤ **128**; no reuse restriction; not equal to the email local-part. Hashed bcrypt cost 12. | `VALIDATION_ERROR` |
| `display_name` | register, profile update | ✅ | **2–50** chars (③); letters/marks/spaces/`.` `'` `-` and digits; no URL-like strings. This is also the "nickname". | `VALIDATION_ERROR` |
| `role` | `POST /auth/role` | ✅ | ∈ {`CUSTOMER`, `PROFESSIONAL`} (never `MODERATOR` via this path); settable once. | `VALIDATION_ERROR`; `ROLE_IMMUTABLE` |
| `token` (verify / reset / unsubscribe) | verify-email, reset-password, unsubscribe | ✅ | opaque string, exact match, single-use, unexpired (verify 24 h, reset 15 min). | `VERIFICATION_TOKEN_INVALID` / `RESET_TOKEN_INVALID` |
| `refresh_token` | refresh, logout | ✅ | opaque; looked up by hash. | `REFRESH_TOKEN_INVALID` / `REFRESH_TOKEN_REUSED` |
| OAuth `code` / `state` | google/callback | ✅ | validated by the OIDC library; `state` must match the issued value. | `VALIDATION_ERROR` |
| account deletion | `DELETE /me` | — | no body; rejected if the caller has an active ban/suspension or an open report against them. | `ACCOUNT_DELETION_BLOCKED` (409) |


## 3. Profiles
---

| Field | Applies to | Required | Constraints | Error |
|---|---|---|---|---|
| `phone` | customer, professional | ❌ | E.164-style; country code + national number validated against the selected country's format; ≤ **20** chars. | `VALIDATION_ERROR` |
| `city` (customer) | customer profile | ❌ | ≤ **100** chars; free text (no gazetteer). | `VALIDATION_ERROR` |
| `bio` | customer, professional | ❌ | ≤ **2000** chars (`bio_max_length` config). | `VALIDATION_ERROR` |
| `preferred_category_ids` | customer profile | ❌ | array of UUID; each must exist in `categories`; ≤ **50** entries; frontend hint only. | `VALIDATION_ERROR` |
| `avatar` (file) | customer, professional | ❌ | exactly one; image only; ≤ **5 MB** (`avatar_max_mb` config). See §9. | `FILE_TOO_LARGE` / `INVALID_FILE_TYPE` |
| `years_experience` | professional | ❌ | integer **0–70** (①); no `date_of_birth` field — flat cap chosen in MVP. | `VALIDATION_ERROR` |
| `hourly_rate` | professional | ❌ | `numeric(10,2)`; ≥ 0; sanity cap ≤ **100000**; ≤ 2 decimals. | `VALIDATION_ERROR` |
| `category_ids` | professional | conditional | array of UUID; each exists; **≥ 1 to reach/keep `ACTIVE`**; edits may replace but not clear the last one; no max count. | `VALIDATION_ERROR`; `CATEGORY_REQUIRED` |
| `cities` | professional | conditional | array of strings; each ≤ **100** chars; **≥ 1 to reach/keep `ACTIVE`**; replace-not-clear; case-insensitive dedupe; no max count. | `VALIDATION_ERROR`; `CITY_REQUIRED` |
| `working_hours_start` / `_end` | professional | ❌ | `HH:mm`; if both set, `start < end`; single fixed schedule (no per-weekday). | `VALIDATION_ERROR` |
| `available` | professional | ❌ | boolean; defaults `false` until set. | — |
| `caption` (portfolio photo) | `POST /me/portfolio` | ❌ | ≤ **2000** chars. | `VALIDATION_ERROR` |


## 4. Orders
---

| Field | Required | Constraints | Error |
|---|---|---|---|
| `category_id` | ✅ | UUID; must exist. | `VALIDATION_ERROR` |
| `title` | ✅ | **5–120** chars (②). | `VALIDATION_ERROR` |
| `description` | ✅ | **20–5000** chars (②). | `VALIDATION_ERROR` |
| `budget_min` / `budget_max` | ❌ | `numeric(10,2)`; ≥ 0; if both present, `min ≤ max`; ≤ 2 decimals. | `VALIDATION_ERROR` |
| `location` | ✅ | city string; ≤ **100** chars. | `VALIDATION_ERROR` |
| `preferred_date` | ❌ | date; must be today or later (⑥). | `VALIDATION_ERROR` |
| `version` (on `PATCH`) | ✅ | integer; must equal the current row version (optimistic lock). | `CONFLICT` |
| `additional_days` (extend) | ✅ | integer **1–90**. | `VALIDATION_ERROR` |
| default expiry window | — | `order_default_expiry_days` config, default **7**. | — |

State-guarded (service layer, not Bean Validation): edit/delete only in the allowed status → `ORDER_NOT_DRAFT` / `ORDER_CLOSED`; publish only from `DRAFT` → `ORDER_NOT_DRAFT`; publish/respond require `status = ACTIVE` on the account → `EMAIL_NOT_VERIFIED`.


## 5. Responses
---

| Field | Required | Constraints | Error |
|---|---|---|---|
| `quote_price` | ✅ | `numeric(10,2)`; ≥ 0; ≤ **1000000** (sanity cap); ≤ 2 decimals. | `VALIDATION_ERROR` |
| `message` | ❌ | ≤ **2000** chars (②). | `VALIDATION_ERROR` |
| — | — | one per `(order_id, professional_id)` — second submit edits; only while order `ACTIVE` and not expired; professional's `profile_status` must be `ACTIVE`. | `ORDER_CLOSED` / `ORDER_EXPIRED` / `PROFILE_INCOMPLETE` |


## 6. Reviews & replies
---

| Field | Required | Constraints | Error |
|---|---|---|---|
| `reviewee_id` | ✅ | UUID; must exist; **≠ the caller**; caller and reviewee roles must be opposite (customer↔professional). | `VALIDATION_ERROR`; `NOT_FOUND` |
| `rating` | ✅ | integer **1–5**. | `VALIDATION_ERROR` |
| `text` | ❌ | ≤ **2000** chars (②); passes the sync moderation check ([10 § D1](10%20-%20Sequence%20Diagrams.md)). | `VALIDATION_ERROR` |
| `photo_file_ids` | ❌ | array of UUID; ≤ **10**; each an image file owned by the caller, ≤ **10 MB**. | `VALIDATION_ERROR` / `FILE_TOO_LARGE` |
| reply `text` | ✅ | **1–2000** chars (②); only on `CUSTOMER`-direction reviews. | `VALIDATION_ERROR`; `FORBIDDEN` |
| — | — | one review per `(reviewer_id, reviewee_id)` — second submit edits. | — |


## 7. Messaging
---

| Field | Required | Constraints | Error |
|---|---|---|---|
| `text` | conditional | ≤ **200** chars; required **iff** no attachment. | `VALIDATION_ERROR` |
| `attachment` (file) | conditional | image ≤ **10 MB**, any other type ≤ **25 MB**; **not** an executable (`.exe .bat .cmd .sh .msi .scr .com .jar` + their MIME types); served `Content-Disposition: attachment`. | `FILE_TOO_LARGE` / `INVALID_FILE_TYPE` |
| — | — | at least one of `text` / `attachment` present; sender not in a `blocks` relationship with the recipient. | `VALIDATION_ERROR`; `USER_BLOCKED` |
| `conversation_id` / `professional_id` (start) | ✅ | UUID; the professional must exist; a conversation is `getOrCreate`d by the `(customer, professional)` pair. | `NOT_FOUND` |


## 8. Reports, appeals, blocking, moderation
---

| Field | Required | Constraints | Error |
|---|---|---|---|
| `reported_user_id` | ✅ | UUID; exists; **≠ caller**; ≤ 1 report per `(reporter, reported)` pair per rolling 24 h. | `CANNOT_REPORT_SELF`; `DUPLICATE_REPORT`; `NOT_FOUND` |
| `reason` | ✅ | ∈ {`no_show`, `poor_quality`, `non_payment`, `harassment`, `scam`, `inappropriate_content`, `other`}. | `VALIDATION_ERROR` |
| `description` | ✅ | **10–2000** chars (②). | `VALIDATION_ERROR` |
| `evidence[]` | ❌ | ≤ **20** files; each ≤ **10 MB**; image / `application/pdf` / `text/plain`. | `FILE_TOO_LARGE` / `INVALID_FILE_TYPE` |
| decision `decision` | ✅ | ∈ {`dismissed`, `warning`, `suspended`, `banned`, `content_removed`}. | `VALIDATION_ERROR` |
| decision `suspension_days` | conditional | integer **1–365**; required iff `decision = suspended`; presets 3/7/14/30 are UI hints only. | `VALIDATION_ERROR` |
| decision `notes` | ❌ | ≤ **4000** chars. | `VALIDATION_ERROR` |
| — | — | `suspended` / `banned` only if target `users.status = ACTIVE`. | `MODERATION_TARGET_NOT_VERIFIED` |
| ban-appeal `text` | ❌ | ≤ **2000** chars (②); within 30 days of the ban; one per ban. | `APPEAL_WINDOW_EXPIRED`; `APPEAL_ALREADY_EXISTS` |
| appeal decision `outcome` | ✅ | ∈ {`upheld`, `rejected`}; reviewer ≠ the banning moderator. | `SAME_MODERATOR_REVIEW` |
| block target | ✅ | UUID; exists; **≠ caller**; idempotent (re-block is a no-op). | `VALIDATION_ERROR`; `NOT_FOUND` |
| `category` `name` (moderator) | ✅ | **2–50** chars (③); case-insensitive unique; delete blocked while referenced. | `VALIDATION_ERROR`; `CONFLICT` |


## 9. Files & uploads
---

Validated by `FileStorageService` ([7 § 1.6](7%20-%20Application%20Classes.md)), not Bean Validation.

| Check | Rule |
|---|---|
| **Size** (per purpose, all config) | avatar 5 MB · portfolio photo 15 MB · review evidence 10 MB · report evidence 10 MB · message image 10 MB · message other 25 MB → `FILE_TOO_LARGE` |
| **Type** (by magic bytes, **not** extension or client `Content-Type`) | avatar / portfolio / review photo → `image/jpeg`, `image/png`, `image/webp`, `image/heic`, `image/heif`, `image/gif` only · report evidence → images + `application/pdf` + `text/plain` · message attachment → anything **except** executables → `INVALID_FILE_TYPE` |
| **Image pipeline** | strip EXIF/all metadata (apply orientation to pixels first); re-encode JPEG q82, keep format; avatar → 512×512 center-crop + 128×128 thumb; others → max 2048 px long edge |
| **Storage down** | `503 STORAGE_UNAVAILABLE` — not queued |
| **Per-user quota** | `max_total_storage_per_user_mb` config — unlimited in MVP |
| Not in MVP | virus scanning, max-concurrent-uploads limit |


## 10. Device tokens & notification preferences
---

| Field | Required | Constraints | Error |
|---|---|---|---|
| device `token` | ✅ | non-blank; ≤ **4096** chars (implementation default — FCM tokens are ~160 chars); upsert on the token value. | `VALIDATION_ERROR` |
| device `platform` | ✅ | ∈ {`ios`, `android`, `web`}. | `VALIDATION_ERROR` |
| preference rows | ✅ | each `{event_type, channel, enabled}`: `event_type` a known routing key ([9](9%20-%20Event%20Catalog.md)); `channel` ∈ {`push`, `email`, `in_app`}; `enabled` boolean. Unknown `event_type` → ignored (⑤). | `VALIDATION_ERROR` |


## 11. Search parameters (`GET /search/professionals`)
---

| Param | Constraints |
|---|---|
| `q` | ❌; ≤ **100** chars; trimmed. |
| `category_id` | ❌; UUID; must exist. |
| `city` | ❌; ≤ 100 chars; normalized for matching. |
| `rating_min` | ❌; **0.0–5.0**, 0.1 steps ([FR-Search-3](2%20-%20Requirements.md)). |
| `price_min` / `price_max` | ❌; ≥ 0; `min ≤ max`. |
| `experience` | ❌; integer ≥ 0. |
| `available` | ❌; boolean. |
| `sort` | ❌; ∈ {`recommended`, `rating`, `price`, `newest`, `popularity`}; default `recommended`. |
| `page` / `size` | per §1 pagination. |


## 12. Gaps resolved (v1.1)
---

All previously undefined constraints have been decided and moved inline (marked with superscript numbers below):

| # | Constraint | Adopted resolution |
|---|---|---|
| ① | **`years_experience` bound** — no `date_of_birth` field in schema, so age-based rule dropped; flat 0–70 cap chosen instead. `date_of_birth` can be added later if needed (has privacy cost). | `years_experience: 0–70 integer` |
| ② | **Text-length limits** for `order.title`, `order.description`, `response.message`, `review.text`, `review_reply.text`, `report.description`, `ban_appeal.text` — **all now specified**. | title 5–120; desc 20–5000; message 2000; review 2000; appeal 2000; report 10–2000 |
| ③ | **`display_name` format** — 2–50 chars, letters/digits/marks/spaces/`. ' -`; no URL-like strings. Also used as nickname. | `display_name: /^[a-zA-Z0-9 .'`\-]{2,50}$/` (no URLs) |
| ④ | **Pagination out-of-range** — clamp (don't error). `page` ≥ 1, `size` 1–100. | clamp on both |
| ⑤ | **Unknown JSON fields** — ignore instead of rejecting. | Jackson `FAIL_ON_UNKNOWN_PROPERTIES = false` |
| ⑥ | **`preferred_date`** — allow same-day, reject past dates. | "today or later" |


## Changelog
---

| Version | Date | Change |
|---|---|---|
| 1.2 | 2026-09-10 | Standardised to the shared doc format. Cleared the last two stray `(proposed)` markers the v1.1 note claimed were already removed (unknown-JSON-fields row → "gap ⑤"; device-token length → "implementation default"). |
| 1.1 | 2026-09-07 | **All 6 gaps resolved.** Inline fields reference the decision # (①–⑥) in §12. Adopted: flat 0–70 cap for `years_experience` (no `date_of_birth` field); text-length limits for 8 fields; `display_name` character class; clamp on pagination; ignore unknown JSON fields; `preferred_date` = today or later. |
| 1.0 | 2026-09-07 | Initial. Global rules + per-field tables for auth, profiles, orders, responses, reviews, messaging, reports/appeals/blocking/moderation, files, device tokens/preferences, search params. 6 spec gaps identified. |
