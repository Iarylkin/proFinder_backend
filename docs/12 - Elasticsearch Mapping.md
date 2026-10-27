# ProFinder — Elasticsearch Mapping

- **Version:** 1.1
- **Date:** 2026-09-10
- **Status:** Stable
- **Purpose:** The two Elasticsearch indexes — `professionals` and `orders` — their field mappings, analyzers, how each document is assembled from PostgreSQL, and how the queries behind [FR-Search](2%20-%20Requirements.md) map onto them. [Documentation Roadmap.md](Documentation%20Roadmap.md) Tier 2 "Elasticsearch mapping".

**Sources:** [4 - Business logic.md § Search and Discovery / § Professional Rating / § Elasticsearch Sync](4%20-%20Business%20logic.md), [2 - Requirements.md § FR-Search](2%20-%20Requirements.md), [6 - Database Schema.md](6%20-%20Database%20Schema.md), [8 - API Specification.md § Search](8%20-%20API%20Specification.md), [9 - Event Catalog.md](9%20-%20Event%20Catalog.md) (`search.professional_reindex`, `search.order_reindex`), [7 - Application Classes.md § 1.5 Search](7%20-%20Application%20Classes.md) (`SearchIndexConsumer`, `SearchService`).

---

## 1. Principles
---

- **PostgreSQL is the source of truth.** Elasticsearch holds a **denormalized projection** built for one screen each: the professional search results, and the professional's "browse open orders" list.
- **Only searchable rows are indexed.** A professional document exists **iff** `users.status = ACTIVE` **and** `professional_profiles.profile_status = ACTIVE`. An order document exists **iff** `orders.status = ACTIVE` **and** `expires_at > now()`. Anything else → the consumer **deletes** the document.
- **Idempotent upserts.** Every write uses `version_type: external` with the row's `version` column, so an out-of-order redelivery can never overwrite newer data ([9 §5](9%20-%20Event%20Catalog.md)).
- **Precompute sort keys at index time.** `sort_rating` (Bayesian) and `popularity` are computed by the consumer and stored as fields — no `script_score` at query time.
- **Single node in MVP:** `number_of_shards: 1`, `number_of_replicas: 0` (bump replicas to 1 when a second node exists). Default `refresh_interval` (1 s) is fine — search lag of ~1 s after a profile edit is acceptable.
- **English-only analysis in MVP** (`NFR-UX-4` — i18n is "extensible later"). The `english` analyzer (stemming + stopwords) is used for free-text fields so "plumbing" matches "plumber".


## 2. Shared analysis settings
---

Both indexes use these. Defined once per index under `settings.analysis`.

```json
{
  "settings": {
    "number_of_shards": 1,
    "number_of_replicas": 0,
    "analysis": {
      "normalizer": {
        "city_normalizer": {
          "type": "custom",
          "filter": ["lowercase", "asciifolding", "trim"]
        }
      },
      "analyzer": {
        "text_en": {
          "type": "custom",
          "tokenizer": "standard",
          "filter": ["lowercase", "asciifolding", "english_stop", "english_stemmer"]
        },
        "name_analyzer": {
          "type": "custom",
          "tokenizer": "standard",
          "filter": ["lowercase", "asciifolding"]
        }
      },
      "filter": {
        "english_stop":    { "type": "stop", "stopwords": "_english_" },
        "english_stemmer": { "type": "stemmer", "language": "english" }
      }
    }
  }
}
```

- `city_normalizer` — a **keyword normalizer** (not an analyzer): "New York ", "new york", "néw york" all become `new york` so exact-match city filters are forgiving of case/spacing/accents but stay exact (no partial matching, per [FR-Search-2](2%20-%20Requirements.md)).
- `name_analyzer` — folded + lowercased but **not stemmed** (stemming a person's name is wrong).
- `text_en` — full English analysis for `bio`, `category_names`, order `title` / `description`.


## 3. `professionals` index
---

### 3.1 Mapping
---

```json
{
  "mappings": {
    "dynamic": "strict",
    "properties": {
      "professional_id":  { "type": "keyword" },

      "display_name":     { "type": "text", "analyzer": "name_analyzer",
                            "fields": { "keyword": { "type": "keyword", "ignore_above": 128 } } },
      "bio":              { "type": "text", "analyzer": "text_en" },

      "category_ids":     { "type": "keyword" },
      "category_names":   { "type": "text", "analyzer": "text_en" },
      "cities":           { "type": "keyword", "normalizer": "city_normalizer" },

      "hourly_rate":      { "type": "scaled_float", "scaling_factor": 100, "null_value": null },
      "years_experience": { "type": "integer" },
      "available":        { "type": "boolean" },

      "rating_avg":       { "type": "scaled_float", "scaling_factor": 100 },
      "review_count":     { "type": "integer" },
      "sort_rating":      { "type": "scaled_float", "scaling_factor": 1000 },
      "popularity":       { "type": "scaled_float", "scaling_factor": 1000 },

      "avatar_thumb_url": { "type": "keyword", "index": false },

      "created_at":       { "type": "date" },
      "last_active_at":   { "type": "date" },

      "version":          { "type": "long", "index": false },
      "indexed_at":       { "type": "date" }
    }
  }
}
```

`dynamic: "strict"` — an unmapped field in a document is a hard error, not a silent mapping explosion. New fields go through this file.

### 3.2 Field notes
---

| Field | Origin | Used for |
|---|---|---|
| `professional_id` | `professional_profiles.user_id` | document `_id`, lookups |
| `display_name` | `users.display_name` | BM25 relevance (`name_analyzer`); `.keyword` for the `newest`/name tie-break and aggregations |
| `bio` | `users.bio` | BM25 relevance only |
| `category_ids` | `professional_categories.category_id[]` | **hard filter** — [FR-Search-1](2%20-%20Requirements.md) exact category match |
| `category_names` | `categories.name[]` (joined) | BM25 relevance — so a text query "electrician" hits the category even if it's not in the bio |
| `cities` | `professional_cities.city[]` | **hard filter** — [FR-Search-2](2%20-%20Requirements.md) exact city (normalized) |
| `hourly_rate` | `professional_profiles.hourly_rate` | **range filter** ([FR-Search-4](2%20-%20Requirements.md)); `price` sort. `null` when the professional hasn't set one → excluded from any price filter, sorts last on `price` |
| `years_experience` | `professional_profiles.years_experience` | **filter** ([FR-Search-7](2%20-%20Requirements.md), exact or `gte`); `null` → excluded from the experience filter |
| `available` | `professional_profiles.available` | **filter** ([FR-Search-5](2%20-%20Requirements.md)); `function_score` boost; primary sort key in "Recommended" |
| `rating_avg` | `professional_profiles.rating_avg` (denormalized) | display; the `rating` sort (raw mean, [FR-Search-9](2%20-%20Requirements.md)) |
| `review_count` | `professional_profiles.review_count` | display; tie-break in "Recommended"; input to `popularity` |
| `sort_rating` | **computed at index time** — `(C·m + rating_avg·review_count) / (C + review_count)` | the "Recommended" sort key and the `function_score` multiplier for text search. `C` and `m` are config (`bayes_prior_count` ≈ 5, `bayes_prior_mean` ≈ platform mean). Never shown to users. |
| `popularity` | **computed at index time** — `rating_avg · log10(review_count + 1)` | the `popularity` sort ([FR-Search-9](2%20-%20Requirements.md)) |
| `avatar_thumb_url` | derived from `users.avatar_file_id` → the 128×128 thumb key | result-card display only, `index: false` |
| `created_at` | `professional_profiles.created_at` | the `newest` sort |
| `last_active_at` | `users.updated_at` (proxy) or a dedicated last-login timestamp | final tie-break in "Recommended" ("most recently active") |
| `version` | `professional_profiles.version` | external-version guard, `index: false` |

### 3.3 Result card
---

The API returns **only** these fields to `SearchResultCardDto` ([FR-Search-10](2%20-%20Requirements.md)) — request them with `_source` filtering:

`professional_id`, `display_name`, `avatar_thumb_url`, `rating_avg` (rounded to 1 decimal for display), `review_count`, `hourly_rate`. `cities` / `available` are cheap to include if the frontend wants them. Everything else (full bio, portfolio, contacts, working hours) loads from Core API when the profile is opened.

### 3.4 Document assembly — `SearchIndexConsumer` on `search.professional_reindex`
---

```text
on message { professional_id, version, reason }:
    dedupe: INSERT processed_events (event_id, "search-indexer") -- skip if present

    row = SELECT pp.*, u.display_name, u.bio, u.status, u.avatar_file_id, u.updated_at
          FROM professional_profiles pp JOIN users u ON u.id = pp.user_id
          WHERE pp.user_id = :professional_id

    IF row IS NULL OR row.status != 'ACTIVE' OR row.profile_status != 'ACTIVE':
        DELETE professionals/_doc/:professional_id        (404 is fine)
        return

    cats  = SELECT c.id, c.name FROM professional_categories pc
            JOIN categories c ON c.id = pc.category_id
            WHERE pc.professional_id = :professional_id
    cities = SELECT city FROM professional_cities WHERE professional_id = :professional_id

    C = config.bayes_prior_count ; m = config.bayes_prior_mean
    sort_rating = (C*m + row.rating_avg * row.review_count) / (C + row.review_count)
    popularity  = row.rating_avg * log10(row.review_count + 1)

    PUT professionals/_doc/:professional_id?version=:version&version_type=external
    {
      professional_id, display_name, bio,
      category_ids: cats.id[], category_names: cats.name[], cities: cities[],
      hourly_rate, years_experience, available,
      rating_avg, review_count, sort_rating, popularity,
      avatar_thumb_url, created_at, last_active_at: row.updated_at,
      version, indexed_at: now()
    }
    -- a 409 (version conflict) is expected and ignored: a newer write already won
```

The **nightly full reindex** runs the same builder over `SELECT user_id FROM professional_profiles WHERE profile_status='ACTIVE'` (join `users.status='ACTIVE'`), reconciling any events lost during an ES outage.

### 3.5 Query shapes
---

| `sort` value | ES query |
|---|---|
| `recommended` (default, no `q`) | `bool.filter` (see below), sorted `available` desc → `sort_rating` desc → `review_count` desc → `last_active_at` desc |
| `rating` | filters + sort `rating_avg` desc |
| `price` | filters + sort `hourly_rate` asc, `missing: _last` |
| `newest` | filters + sort `created_at` desc |
| `popularity` | filters + sort `popularity` desc |
| text query present (`q`) | `function_score`: `multi_match` (`q` over `display_name^2`, `bio`, `category_names`) as the query, `field_value_factor` on `sort_rating` with `modifier: ln1p`, plus a `filter` clause boosting `available`. `bool.filter` still applied. Overrides `sort` (relevance wins) unless the user explicitly re-picks a sort. |

**`bool.filter` clauses** (all optional, all hard filters — they never affect score):
- `term  cities = {city}` (normalized)
- `term  category_ids = {category_id}`
- `range hourly_rate >= {price_min}` / `<= {price_max}`
- `term  available = true` (only when the availability filter is on)
- `range rating_avg >= {rating_min}` (0.0–5.0, 0.1 steps — [FR-Search-3](2%20-%20Requirements.md))
- `term years_experience = {experience}` (or `range >=` — [FR-Search-7](2%20-%20Requirements.md) says "exact"; treat the value as a minimum only if product later wants a slider)

Pagination: `from = (page-1)*size`, `size` (default 20, max 100). Result pages (first ~3) are cached in Redis 60 s keyed by the normalized filter hash ([4 § Search Result Caching](4%20-%20Business%20logic.md)).


## 4. `orders` index
---

Backs the professional's "browse open orders" screen (`GET /orders?category_id=&city=&budget_min=&budget_max=` — [8](8%20-%20API%20Specification.md)). Customers do **not** hit this index (they see their own orders straight from PostgreSQL). The `order.published` fan-out to matching professionals in the Notification Service is also a **PostgreSQL** query, not this index.

> **Why index orders at all** at ~200-user scale, when PostgreSQL could serve this filter? To keep one search stack, and because the professional's browse screen shares pagination / caching / "matching" semantics with professional search. If load stays tiny this index could be dropped and the endpoint served from PG — noted as a possible simplification, not a change now.

### 4.1 Mapping
---

```json
{
  "mappings": {
    "dynamic": "strict",
    "properties": {
      "order_id":      { "type": "keyword" },
      "customer_id":   { "type": "keyword" },

      "category_id":   { "type": "keyword" },
      "category_name": { "type": "text", "analyzer": "text_en" },
      "city":          { "type": "keyword", "normalizer": "city_normalizer" },

      "title":         { "type": "text", "analyzer": "text_en" },
      "description":   { "type": "text", "analyzer": "text_en" },

      "budget_min":    { "type": "scaled_float", "scaling_factor": 100, "null_value": null },
      "budget_max":    { "type": "scaled_float", "scaling_factor": 100, "null_value": null },

      "preferred_date":{ "type": "date" },
      "published_at":  { "type": "date" },
      "expires_at":    { "type": "date" },

      "version":       { "type": "long", "index": false },
      "indexed_at":    { "type": "date" }
    }
  }
}
```

`title` / `description` are indexed as analyzed text so a future keyword search over orders is a mapping no-op; the current endpoint is **filter-only** (no `q` param yet).

### 4.2 Document assembly — `SearchIndexConsumer` on `search.order_reindex`
---

```text
on message { order_id, version, reason }:
    dedupe on (event_id, "search-indexer")

    row = SELECT o.*, c.name AS category_name
          FROM orders o JOIN categories c ON c.id = o.category_id
          WHERE o.id = :order_id

    IF row IS NULL OR row.status != 'ACTIVE' OR row.expires_at <= now():
        DELETE orders/_doc/:order_id
        return

    PUT orders/_doc/:order_id?version=:version&version_type=external
    { order_id, customer_id, category_id, category_name, city: row.location,
      title, description, budget_min, budget_max, preferred_date,
      published_at, expires_at, version, indexed_at: now() }
```

- `reason` values ([9 §5](9%20-%20Event%20Catalog.md)): `published` / `edited` → upsert; `closed` / `expired` → the guard above deletes; `extended` → upsert (a previously-expired order comes back).
- A **scheduled sweep** (hourly) also deletes documents whose `expires_at` has passed, in case the `order.expired` event was lost. Cheap: `DELETE _query { range: { expires_at: { lte: now } } }`.

### 4.3 Query shape
---

Professional browsing open orders:

```text
bool:
  filter:
    - term  category_id = {category_id}        # constrained to one of the pro's own categories
    - term  city         = {city}              # one of the pro's own cities
    - range budget_max  >= {budget_min}?       # overlap logic if the pro filters by budget
    - range budget_min  <= {budget_max}?
    - range expires_at   > now                 # belt-and-suspenders
sort: published_at desc
```

`category_id` / `city` are validated against the professional's own `professional_categories` / `professional_cities` before the query runs (a pro can't browse categories they don't offer).


## 5. Index lifecycle & ops
---

- **Creation:** both indexes are created by a startup migration (or an idempotent `PUT /{index}` guarded by `HEAD`). Mappings live in `src/main/resources/es/professionals.json` / `orders.json`.
- **Mapping changes:** additive fields → `PUT /{index}/_mapping`. Breaking changes → create `professionals_v2`, reindex, flip a read alias `professionals` → `professionals_v2`. Use a **write alias** from day 1 (`professionals_write`) so this is painless later.
- **Aliases:** `professionals` (read), `professionals_write` (write) → both point at `professionals_000001` initially.
- **Nightly full reindex:** a `@Scheduled` job in Core API, ~03:00, streams all `ACTIVE` professionals / `ACTIVE` non-expired orders through the same document builders. This is the only reconciliation mechanism — there is no PostgreSQL fallback for reads ([FR-Search-11](2%20-%20Requirements.md)).
- **ES down:** reindex events accumulate on the `search.index` queue (no TTL, no DLQ — [9 §3](9%20-%20Event%20Catalog.md)); search endpoints return `503 SEARCH_UNAVAILABLE`.


## Changelog
---

| Version | Date | Change |
|---|---|---|
| 1.1 | 2026-09-10 | Standardised to the shared doc format (metadata, separators). |
| 1.0 | 2026-09-07 | Initial. `professionals` + `orders` mappings, shared analyzers (`text_en`, `name_analyzer`, `city_normalizer`), per-field origin/use tables, document-assembly pseudocode for both `search.*_reindex` consumers, query shapes for every `sort` mode and the professional order-browse, index lifecycle/alias/reindex ops. |
